package dev.memphis.sdk.consumer

import dev.memphis.sdk.Lifecycle
import dev.memphis.sdk.Memphis
import dev.memphis.sdk.MemphisError
import dev.memphis.sdk.Message
import dev.memphis.sdk.toInternalName
import dev.memphis.sdk.toStringAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.toJavaDuration

internal class ConsumerImpl(
    private val memphis: Memphis,
    override val name: String,
    val stationName: String,
    override val group: String,
    override val pullInterval: Duration,
    override val batchSize: Int,
    override val batchMaxTimeToWait: Duration,
    override val maxAckTime: Duration,
    override val maxMsgDeliveries: Int,
    override val startConsumeFromSequence: Int? = null,
    override val lastMessages: Int? = null,
    override val partitionNumber: Int = 1,
    override val username: String,
    override val applicationId: String,
) : Lifecycle, Consumer {
    private val logger = KotlinLogging.logger {}

    private var consumingStatus = ConsumingStatus.INACTIVE
    private var partitionsList: List<Int>? = null

    private var jobs: List<Job> = emptyList()

    private var keepAliveError: Boolean = false

    override suspend fun consume(vararg partitions: Int): Flow<Message> {
        setConsumeActive()
        return merge(startConsumeLoop(*partitions), startDlsLoop())


    }

    private suspend fun startDlsLoop() = channelFlow {
        subscribeDls {
            logger.debug { "Received DLQ Message: ${it.data} Headers: ${it.headers}" }
            send(it)
        }
    }

    private suspend fun startConsumeLoop(vararg partitions: Int) = channelFlow {
        logger.debug { "In Consume loop" }
        keepAliveError = false
        val pullOptions = io.nats.client.PullSubscribeOptions.builder()
            .durable(group)
            .build()
        var context = memphis.brokerConnection.jetStream()

        logger.debug { "Setting up jobs" }
        // This finds the list of partitions either from the called parameter
        // or from options.  If the option value is -1 it will subscribe to all
        jobs = (partitions.takeIf { it.isNotEmpty() }?.let { partitions.toTypedArray().toList() }
            ?: partitionNumber.takeIf { partitionNumber == -1 }?.let { partitionsList }
            ?: listOf(partitionNumber)).flatMap { partition ->
            val partitionName = "${stationName.toInternalName()}\$${partition}${dev.memphis.sdk.Memphis.STATION_SUFFIX}"
            listOf(
                launch {
                    logger.debug { "Starting subscription for $partitionName" }
                    var sub = context.subscribe(partitionName, pullOptions)
                    while (true) {
                        sub.fetch(batchSize, batchMaxTimeToWait.toJavaDuration()).forEach {
                            logger.trace { "Received Message: ${String(it.data)} Headers: ${it.headers.toStringAll()}" }
                            send(it.toSdkMessage())
                        }
                        delay(pullInterval)
                    }
                },
            )
        }
        jobs = jobs + listOf(launch {
            delay(10000)
            try {
                memphis.brokerConnection.jetStreamManagement()
                    .getConsumerInfo("$stationName\$1", group)
            } catch (e: Exception) {
                when (e) {
                    is InterruptedException, is IllegalStateException -> {}
                    is RuntimeException -> throw e
                    else -> {
                        keepAliveError = true
                        throw RuntimeException(e.message, e)
                    }
                }

            }
        })
    }

    private fun setConsumeActive() {
        logger.info { "Current status ${consumingStatus}" }
        if (consumingStatus == ConsumingStatus.ACTIVE) throw MemphisError("Already consuming")
        consumingStatus = ConsumingStatus.ACTIVE
    }

    private fun io.nats.client.Message.toSdkMessage() = Message(this, memphis)

    override suspend fun subscribeMessages(vararg partitions: Int): Flow<Message> = flow {
        setConsumeActive()
        while (currentCoroutineContext().isActive) {
            startConsumeLoop(*partitions).collect {
                emit(it)
            }
        }

    }

    override suspend fun subscribeDls() = flow {
        setConsumeActive()
        while (currentCoroutineContext().isActive) {
            startDlsLoop().collect {
                emit(it)
            }
        }

    }


    private suspend fun subscribeDls(callback: suspend (msg: Message) -> Unit) {
        memphis.brokerDispatch.subscribe(
            "\$memphis_dls_${stationName}.${group}",
            "\$memphis_dls_${stationName}.${group}"
        )
        {
            runBlocking {
                callback(it.toSdkMessage())
            }
        }
    }

    override fun stopConsuming() {
        if (consumingStatus == ConsumingStatus.INACTIVE) throw MemphisError("Consumer is inactive")
        jobs.forEach(Job::cancel)
        consumingStatus = ConsumingStatus.INACTIVE
    }

    override suspend fun destroy() {
        if (consumingStatus == ConsumingStatus.ACTIVE) {
            stopConsuming()
        }
        memphis.destroyResource(this)
    }

    override fun getCreationSubject(): String = "${'$'}memphis_consumer_creations"

    override fun getCreationRequest(): JsonObject = buildJsonObject {
        put("name", name)
        put("station_name", stationName)
        put("connection_id", memphis.connectionId)
        put("consumer_type", "application")
        put("consumers_group", group)
        put("max_ack_time_ms", maxAckTime.inWholeMilliseconds)
        put("max_msg_deliveries", maxMsgDeliveries)
        put("username", username)
        put("start_consume_from_sequence", startConsumeFromSequence)
        put("last_messages", lastMessages)
        put("req_version", 4)
        put("app_id", applicationId)
        put("sdk_lang", "kotlin")
    }

    override fun getDestructionSubject(): String = "${'$'}memphis_consumer_destructions"

    override fun getDestructionRequest(): JsonObject = buildJsonObject {
        put("name", name)
        put("station_name", stationName)
    }

    override suspend fun handleCreationResponse(msg: io.nats.client.Message) {
        val res = try {
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }.decodeFromString<CreateConsumerResponse>(String(msg.data))
        } catch (e: Exception) {
            logger.error(e) { "Processing results" }
            return super.handleCreationResponse(msg)
        }

        if (res.error != "") throw MemphisError(res.error)
        this.partitionsList = res.partitionsUpdate.partitionsList
    }

    enum class SubscriptionStatus {
        INACTIVE,
        ACTIVE
    }

    enum class ConsumingStatus {
        INACTIVE,
        ACTIVE
    }

    @Serializable
    private data class CreateConsumerResponse(
        val error: String,
        @SerialName("partitions_update") val partitionsUpdate: ConsumerImpl.PartitionsUpdate,
    )

    @Serializable
    private data class PartitionsUpdate(
        @SerialName("partitions_list")
        var partitionsList: ArrayList<Int>? = null
    )

}
