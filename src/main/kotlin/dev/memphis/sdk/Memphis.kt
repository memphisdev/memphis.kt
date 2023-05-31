package dev.memphis.sdk

import dev.memphis.sdk.consumer.Consumer
import dev.memphis.sdk.consumer.ConsumerImpl
import dev.memphis.sdk.producer.Producer
import dev.memphis.sdk.producer.ProducerImpl
import dev.memphis.sdk.schemas.Schema
import dev.memphis.sdk.schemas.SchemaLifecycle
import dev.memphis.sdk.station.Station
import dev.memphis.sdk.station.StationImpl
import dev.memphis.sdk.station.StationUpdateManager
import io.nats.client.Connection
import io.nats.client.Dispatcher
import io.nats.client.JetStream
import io.nats.client.Nats
import io.nats.client.PublishOptions
import io.nats.client.PullSubscribeOptions
import io.nats.client.impl.NatsMessage
import java.nio.charset.Charset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.await
import mu.KotlinLogging

class Memphis private constructor(
    val host: String,
    val username: String?,
    val connectionToken: String,
    val password: String,
    val port: Int,
    val autoReconnect: Boolean,
    val maxReconnects: Int,
    val reconnectWait: Duration,
    val connectionTimeout: Duration
) {
    private val logger = KotlinLogging.logger {}

    internal val scope = CoroutineScope(Job())

    val connectionId = generateRandomHex(12)

    internal val brokerConnection: Connection = if (connectionToken.isNotEmpty() && password.isEmpty())
        getTokenBrokerConnection() else getUserPassBrokerConnection()

    internal val brokerDispatch: Dispatcher = brokerConnection.createDispatcher()
    private val jetStream: JetStream = brokerConnection.jetStream()

    internal val stationUpdateManager = StationUpdateManager(brokerDispatch, scope)
    internal val configUpdateManager = ConfigUpdateManager(brokerDispatch, scope)

    fun isConnected() = brokerConnection.status == Connection.Status.CONNECTED

    fun close() {
        scope.cancel()
        brokerConnection.close()
    }

    private fun getTokenBrokerConnection() : Connection {
        return io.nats.client.Options.Builder()
            .server("nats://${host}:${port}")
            .connectionName("$connectionId::${username}")
            .token(connectionToken.toCharArray())
            .connectionTimeout(connectionTimeout.toJavaDuration())
            .reconnectWait(reconnectWait.toJavaDuration())
            .maxReconnects(maxReconnects)
            .let { if (autoReconnect) it else it.noReconnect() }
            .build()
            .let { Nats.connect(it) }
    }
    private fun getUserPassBrokerConnection() : Connection {
        return io.nats.client.Options.Builder()
            .server("nats://${host}:${port}")
            .connectionName("$connectionId::${username}")
            .userInfo(username, password)
            .connectionTimeout(connectionTimeout.toJavaDuration())
            .reconnectWait(reconnectWait.toJavaDuration())
            .maxReconnects(maxReconnects)
            .let { if (autoReconnect) it else it.noReconnect() }
            .build()
            .let { Nats.connect(it) }
    }

    suspend fun consumer(
        stationName: String,
        consumerName: String,
        options: (Consumer.Options.() -> Unit)? = null
    ): Consumer {
        val opts = options?.let { Consumer.Options().apply(it) } ?: Consumer.Options()

        val cName = if (opts.genUniqueSuffix) {
            extendNameWithRandSuffix(consumerName)
        } else {
            consumerName
        }.toInternalName()

        val groupName = (opts.consumerGroup ?: consumerName).toInternalName()

        val pullOptions = PullSubscribeOptions.builder()
            .durable(groupName)
            .build()

        val subscription = jetStream.subscribe("${stationName.toInternalName()}.final", pullOptions)

        val consumerImpl = ConsumerImpl(
            this,
            cName,
            stationName.toInternalName(),
            groupName,
            opts.pullInterval,
            opts.batchSize,
            opts.batchMaxTimeToWait,
            opts.maxAckTime,
            opts.maxMsgDeliveries,
            subscription
        )
        createResource(consumerImpl)
        consumerImpl.pingConsumer()

        return consumerImpl
    }

    suspend fun producer(
        stationName: String,
        producerName: String,
        options: (Producer.Options.() -> Unit)? = null
    ): Producer {
        val opts = options?.let { Producer.Options().apply(it) } ?: Producer.Options()

        val pName = if (opts.genUniqueSuffix) {
            extendNameWithRandSuffix(producerName)
        } else {
            producerName
        }.toInternalName()

        val producer = ProducerImpl(
            this,
            pName,
            stationName.toInternalName()
        )

        stationUpdateManager.listenToSchemaUpdates(stationName.toInternalName())
        try {
            createResource(producer)
        } catch (e: Exception) {
            stationUpdateManager.removeSchemaUpdateListener(stationName.toInternalName())
            e.printStackTrace()
        }

        return producer
    }

    internal fun brokerPublish(message: NatsMessage, options: PublishOptions) =
        jetStream.publishAsync(message, options)


    internal fun getStationSchema(stationName: String): Schema {
        return stationUpdateManager[stationName.toInternalName()].schema
    }

    suspend fun createStation(name: String, options: (Station.Options.() -> Unit)? = null): Station {
        val opts = options?.let { Station.Options().apply(it) } ?: Station.Options()

        val station = StationImpl(
            this,
            name.toInternalName(),
            opts.retentionType,
            opts.retentionValue,
            opts.storageType,
            opts.replicas,
            opts.idempotencyWindow,
            opts.schemaName,
            opts.sendPoisonMsgToDls,
            opts.sendSchemaFailedMsgToDls
        )

        try {
            createResource(station)
        } catch (e: MemphisError) {
            if (e.message!!.contains("already exist")) return station
        }

        return station
    }

    suspend fun attachSchema(schemaName: String, stationName: String) {
        createResource(SchemaLifecycle.Attach(schemaName, stationName))
    }

    suspend fun detachSchema(stationName: String) {
        destroyResource(SchemaLifecycle.Detach(stationName))
    }

    private suspend fun createResource(d: Create) {
        val subject = d.getCreationSubject()
        val req = d.getCreationRequest()
        logger.debug { "Creating: $subject" }

        val data = brokerConnection.request(subject, req.toString().toByteArray()).await()

        d.handleCreationResponse(data)
    }

    internal suspend fun destroyResource(d: Destroy) {
        val subject = d.getDestructionSubject()
        val req = d.getDestructionRequest()

        logger.debug { "Destroying: $subject" }

        val data = brokerConnection.request(subject, req.toString().toByteArray()).await().data

        if (data.isNotEmpty() && !data.toString(Charset.defaultCharset()).contains("not exist")) {
            throw MemphisError(data)
        }
    }

    class Options(
        private val host: String,
        private val username: String,
        private val connectionToken: String?,
        private val password:String?
    ) {
        var port = 6666;
        var autoReconnect = true;
        var maxReconnects = 3;
        var reconnectWait = 5.seconds;
        var connectionTimeout = 15.seconds;

        internal fun build() = Memphis(
            host, username, connectionToken ?: "", password ?: "", port, autoReconnect, maxReconnects, reconnectWait, connectionTimeout
        )
    }
    class Password constructor(
        val password : String
    )

    class ConnectionToken(
        val connectionToken: String
    )

    companion object {
        fun connect(options: Options): Memphis =
            options.build()

        fun connect(host: String, username: String, password: Password): Memphis =
            Options(host, username, null, password.password).build()

        fun connect(host: String, username: String, connectionToken: ConnectionToken): Memphis =
            Options(host, username, connectionToken.connectionToken, null).build()

        fun connect(host: String, username: String, connectionToken: ConnectionToken, options: Options.() -> Unit): Memphis =
            Options(host, username, connectionToken.connectionToken, null).apply(options).build()

        fun connect(host: String, username: String, password: Password, options: Options.() -> Unit): Memphis =
            Options(host, username, null, password.password).apply(options).build()

    }
}
