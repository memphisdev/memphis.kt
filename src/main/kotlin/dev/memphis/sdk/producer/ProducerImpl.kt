package dev.memphis.sdk.producer

import dev.memphis.sdk.Headers
import dev.memphis.sdk.Lifecycle
import dev.memphis.sdk.Memphis
import dev.memphis.sdk.MemphisError
import dev.memphis.sdk.exceptions.MissingBrokerData
import dev.memphis.sdk.getDlsSubject
import dev.memphis.sdk.resources.DlsMessage
import dev.memphis.sdk.resources.SchemaUpdateInit
import dev.memphis.sdk.schemas.Schema
import dev.memphis.sdk.toInternalName
import dev.memphis.sdk.toStringAll
import io.nats.client.Message
import io.nats.client.PublishOptions
import io.nats.client.api.PublishAck
import io.nats.client.impl.NatsMessage
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mu.KotlinLogging
import java.nio.charset.Charset
import kotlin.time.toJavaDuration

class ProducerImpl(
    private val memphis: Memphis,
    override val name: String,
    override val stationName: String,
    override val username: String,
    override val applicationId: String
) : Producer, Lifecycle {

    private val logger = KotlinLogging.logger {}

    private var partitionsList: List<Int>? = null
    private var nextPartitionIndex = 0;
    override val partitionsFunctions = mutableMapOf<Int, Int>()

    override fun produceAsync(message: ByteArray, options: (Producer.ProduceOptions.() -> Unit)?) {
        memphis.scope.launch { callProduce(message, options) }
    }

    override suspend fun produce(message: ByteArray, options: (Producer.ProduceOptions.() -> Unit)?) {
        val res = callProduce(message, options)
        if (res.hasError()) throw MemphisError(res.error)
    }

    private suspend fun callProduce(
        message: ByteArray,
        opts: (Producer.ProduceOptions.() -> Unit)? = null
    ): PublishAck {

        val options = opts?.let { Producer.ProduceOptions().apply(it) } ?: Producer.ProduceOptions()

        options.headers.putUnchecked("${'$'}memphis_connectionId", memphis.connectionId)
        options.headers.putUnchecked("${'$'}memphis_producedBy", name)

        options.messageId?.also { options.headers.putUnchecked("msg-id", it) }

        logger.trace { "Publish Message: ${message.toString(Charset.defaultCharset())} Headers: ${options.headers.headers.toStringAll()}" }

        val partNumber = getPartitionNumber(options)
        val partitionName = partitionsFunctions.get(partNumber)?.let {
            "${stationName.toInternalName()}\$${partNumber}.functions.$it"
        } ?: "${stationName.toInternalName()}\$${partNumber}${Memphis.STATION_SUFFIX}"

        val data = validateMessage(message, options.headers)

        val natsMsg = NatsMessage.builder()
            .subject(partitionName)
            .headers(options.headers.headers)
            .data(data)
            .build()

        val pubOpts = PublishOptions.builder().streamTimeout(options.ackWait.toJavaDuration()).build()
        return memphis.brokerPublish(natsMsg, pubOpts).await()
    }

    private fun getPartitionNumber(options: Producer.ProduceOptions): Int {
        return if (options.partition == -1) {
             partitionsList?.get(nextPartitionIndex).also {
                 nextPartitionIndex = (nextPartitionIndex + 1) % partitionsList!!.size
             } ?: throw MissingBrokerData("Missing partition information")
        } else {
            options.partition
        }

    }

    private suspend fun validateMessage(message: ByteArray, headers: Headers): ByteArray {
        val schema = try {
            getSchema()
        } catch (e: Exception) {
            throw MemphisError("Schema validation has failed", e)
        }

        return try {
            schema.validateMessage(message)
        } catch (e: Exception) {
            sendMessageToDls(message, headers, e)
            throw e
        }
    }

    private suspend fun sendMessageToDls(message: ByteArray, headers: Headers, throwable: Throwable) {
        if (!memphis.configUpdateManager.sendMessageToDls(stationName)) return
        val timeSent = Clock.System.now()
        val dlsHeaders = headers.headers.keySet().associateWith { headers.headers[it].joinToString(" ") }

        val schemaFailMsg = DlsMessage(
            id = dlsMessageId(timeSent),
            stationName = stationName,
            producer = DlsMessage.ProducerDetails(
                name = name,
                connectionId = memphis.connectionId
            ),
            message = DlsMessage.MessagePayloadDls(
                timeSent = timeSent,
                data = String(message),
                headers = dlsHeaders
            ),
            creationDate = timeSent
        )

        val msg = Json.encodeToString(schemaFailMsg)
        memphis.brokerConnection.publish(getDlsSubject("schema", stationName, schemaFailMsg.id), msg.toByteArray())

        if (memphis.configUpdateManager.sendNotification()) {
            sendNotification {
                title = "Schema validation has failed"
                this.message = "Station: $stationName\nProducer: $name\nError: ${throwable.message}"
                code = message.toString(Charset.defaultCharset())
                type = schemaVFailAlertType
            }
        }
    }

    override fun updatePartitionsFunctions(functions: Map<Int, Int>) {
        partitionsFunctions.clear()
        partitionsFunctions.putAll(functions)
    }


    private fun dlsMessageId(timeSent: Instant) =
        "$stationName~$name~0~${timeSent}"
            .replace(" ", "")
            .replace(",", "+")


    private fun sendNotification(block: Notification.() -> Unit) {
        val msg = Notification().apply(block).let { Json.encodeToString(it) }
        memphis.brokerConnection.publish("${'$'}memphis_notifications", msg.toByteArray())
    }

    @Serializable
    private class Notification {
        lateinit var title: String

        @SerialName("msg")
        lateinit var message: String
        lateinit var code: String
        lateinit var type: String
    }

    private fun getSchema(): Schema =
        memphis.getStationSchema(stationName)

    override suspend fun destroy() {
        memphis.destroyResource(this)
    }

    override fun getCreationSubject(): String =
        "${'$'}memphis_producer_creations"

    override fun getCreationRequest(): JsonObject = buildJsonObject {
        put("name", name)
        put("station_name", stationName)
        put("connection_id", memphis.connectionId)
        put("producer_type", "application")
        put("req_version", 4)
        put("username", username)
        put("app_id", applicationId)
        put("sdk_lang", "kotlin")
    }

    override suspend fun handleCreationResponse(msg: Message) {
        val res = try {
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }.decodeFromString<CreateProducerResponse>(String(msg.data))
        } catch (_: Exception) {
            return super.handleCreationResponse(msg)
        }

         if (res.error != "") throw MemphisError(res.error)
        memphis.stationUpdateManager.applySchema(stationName, res.schemaUpdate)
        memphis.configUpdateManager.setClusterConfig("send_notification", res.clusterSendNotification)
        memphis.configUpdateManager.setStationSchemaverseToDls(stationName, res.schemaverseToDls)
        this.partitionsFunctions.clear()
        this.partitionsFunctions.putAll(res.stationPartitionsFirstFunctions)
        this.partitionsList = res.partitionsUpdate.partitionsList
    }

    override fun getDestructionSubject(): String =
        "${'$'}memphis_producer_destructions"

    override fun getDestructionRequest(): JsonObject = buildJsonObject {
        put("name", name)
        put("station_name", stationName)
    }

    @Serializable
    private data class CreateProducerResponse(
        val error: String,
        @SerialName("schema_update") val schemaUpdate: SchemaUpdateInit,
        @SerialName("schemaverse_to_dls") val schemaverseToDls: Boolean,
        @SerialName("send_notification") val clusterSendNotification: Boolean,
        @SerialName("partitions_update") val partitionsUpdate: PartitionsUpdate,
        @SerialName("station_version") val StationVersion: Int,
        @SerialName("station_partitions_first_functions") val stationPartitionsFirstFunctions: Map<Int, Int>
    )

    @Serializable
    private data class PartitionsUpdate(
        @SerialName("partitions_list")
        var partitionsList: ArrayList<Int>? = null
    )

    companion object {
        private const val schemaVFailAlertType = "schema_validation_fail_alert"

    }
}