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
    val username: String,
    authorizationType: AuthorizationType,
    val port: Int,
    val autoReconnect: Boolean,
    val maxReconnects: Int,
    val reconnectWait: Duration,
    val connectionTimeout: Duration
) {
    private val logger = KotlinLogging.logger {}

    internal val scope = CoroutineScope(Job())

    val connectionId = generateRandomHex(12)

    internal val brokerConnection: Connection =
        when (authorizationType) {
            is ConnectionToken -> buildBrokerConnection().token(authorizationType.connectionToken).build().let { Nats.connect(it) }
            is Password -> buildBrokerConnection().userInfo(username, authorizationType.password).build().let { Nats.connect(it) }
            else -> throw MemphisError("Authorization type unrecognized.")
        }

    internal val brokerDispatch: Dispatcher = brokerConnection.createDispatcher()
    private val jetStream: JetStream = brokerConnection.jetStream()

    internal val stationUpdateManager = StationUpdateManager(brokerDispatch, scope)
    internal val configUpdateManager = ConfigUpdateManager(brokerDispatch, scope)

    /**
     * Check if the connection to the Memphis broker is connected.
     * @return [Boolean] True if connected, false otherwise.
     */
    fun isConnected() = brokerConnection.status == Connection.Status.CONNECTED

    /**
     * Close the connection to the Memphis broker.
     * @see [Connection.close]
     */
    fun close() {
        scope.cancel()
        brokerConnection.close()
    }

    private fun buildBrokerConnection() : io.nats.client.Options.Builder {
        return io.nats.client.Options.Builder()
            .server("nats://${host}:${port}")
            .connectionName("$connectionId::${username}")
            .connectionTimeout(connectionTimeout.toJavaDuration())
            .reconnectWait(reconnectWait.toJavaDuration())
            .maxReconnects(maxReconnects)
            .let { if (autoReconnect) it else it.noReconnect() }
    }

    /**
     * Creates a new consumer for the given station.
     * @param stationName The name of the station to consume from.
     * @param consumerName The name of the consumer.
     * @param options The options to use when creating the consumer, see [Consumer.Options].
     * @return [Consumer] The newly created consumer.
     * @throws [MemphisError] If the station does not exist, or if the consumer name is invalid.
     * @see [Consumer.Options]
     * @see [Consumer]
     */
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

    /**
     * Creates a new producer for the given station.
     * @param stationName The name of the station to produce to.
     * @param producerName The name of the producer.
     * @param options The options to use when creating the producer, see [Producer.Options].
     * @return [Producer] The newly created producer.
     * @throws [MemphisError] If the station does not exist, or if the producer name is invalid.
     * @see [Producer.Options]
     * @see [Producer]
     */
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

    /**
     * Creates a new station with the given name and options.
     * @param name The name of the station to create.
     * @param options The options to use when creating the station, see [Station.Options].
     * @return [Station] The newly created station.
     * @throws [MemphisError] If the station already exists, or if the station name is invalid.
     * @see [Station.Options]
     */
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

    /**
     * Attaches a schema to a station given the schema name and station name.
     * @param schemaName The name of the schema to attach.
     * @param stationName The name of the station to attach the schema to.
     * @throws [MemphisError] If the station does not exist, or if the station name is invalid.
     * @see [detachSchema]
     */
    suspend fun attachSchema(schemaName: String, stationName: String) {
        createResource(SchemaLifecycle.Attach(schemaName, stationName))
    }

    /**
     * Detaches the schema from the station given the station name.
     * @param stationName The name of the station to detach the schema from.
     * @throws [MemphisError] If the station does not exist, or if the station name is invalid.
     * @see [attachSchema]
     */
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
        private val authorizationType: AuthorizationType
    ) {
        /**
         * The port to connect to, defaults to 6666.
         */
        var port = 6666

        /**
         * Whether to automatically reconnect to the broker if the connection is lost, defaults to true.
         */
        var autoReconnect = true

        /**
         * The maximum number of reconnect attempts, defaults to 3.
         */
        var maxReconnects = 3

        /**
         * The time to wait between reconnect attempts, defaults to 5 seconds.
         */
        var reconnectWait = 5.seconds

        /**
         * The timeout for connecting to the broker, defaults to 15 seconds.
         */
        var connectionTimeout = 15.seconds

        internal fun build() = Memphis(
            host, username, authorizationType, port, autoReconnect, maxReconnects, reconnectWait, connectionTimeout
        )
    }

    interface AuthorizationType

    class Password (
        /**
         * The password to use for authentication.
         * @see [Memphis.connect]
         */
        val password : String
    ): AuthorizationType

    class ConnectionToken (
        /**
         * The connection token to use for authentication.
         * @see [Memphis.connect]
         */
        val connectionToken: String
    ): AuthorizationType

    companion object {

        /**
         * Connect to a Memphis instance with the given options.
         * @param options A lambda to configure the Memphis instance, see [Options].
         * @return [Memphis] The Memphis instance.
         * @see [Options]
         * @throws [MemphisError] If the connection fails or the authorization is invalid.
         */
        fun connect(options: Options): Memphis =
            options.build()

        /**
         * Connect to a Memphis instance with the given host, username and authorization type.
         * @param host The host of the Memphis instance, e.g. "localhost".
         * @param username The username to use for authentication.
         * @param authorizationType The type of authorization to use, either [Password] or [ConnectionToken].
         * @return [Memphis] The Memphis instance.
         * @see [Password]
         * @see [ConnectionToken]
         * @throws [MemphisError] If the connection fails or the authorization is invalid.
         */
        fun connect(host: String, username: String, authorizationType: AuthorizationType): Memphis =
            Options(host, username, authorizationType).build()

        /**
         * Connect to a Memphis instance with the given host, username, authorization type and options.
         * @param host The host of the Memphis instance, e.g. "localhost".
         * @param username The username to use for authentication.
         * @param authorizationType The type of authorization to use, either [Password] or [ConnectionToken].
         * @param options A lambda to configure the Memphis instance, see [Options].
         * @return [Memphis] The Memphis instance.
         * @see [Password]
         * @see [ConnectionToken]
         * @see [Options]
         * @throws [MemphisError] If the connection fails or the authorization is invalid.
         */
        fun connect(host: String, username: String, authorizationType: AuthorizationType, options: Options.() -> Unit): Memphis =
            Options(host, username, authorizationType).apply(options).build()

    }
}
