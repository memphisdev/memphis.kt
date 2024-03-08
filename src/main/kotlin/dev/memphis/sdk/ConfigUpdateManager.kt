package dev.memphis.sdk

import dev.memphis.sdk.resources.ConfigurationsUpdate
import io.nats.client.Dispatcher
import io.nats.client.MessageHandler
import io.nats.client.Subscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import mu.KotlinLogging

typealias ConfigName = String
typealias StationName = String

internal class ConfigUpdateManager(
    private val dispatcher: Dispatcher,
    private val memphis:Memphis,
    scope: CoroutineScope
) {
    private val logger = KotlinLogging.logger {}

    private val mutex = Mutex()
    private val clusterConfigurations = mutableMapOf<ConfigName, Boolean>()
    private val stationSchemaverseToDls = mutableMapOf<StationName, Boolean>()

    private var subscription: Subscription? = null

    init {
            scope.launch {
                listenToConfigUpdates(dispatcher)
            }
    }

    private fun listenToConfigUpdates(dispatcher: Dispatcher) {
//        subscription = dispatcher.subscribe("\$memphis_sdk_configurations_updates", messageHandler())
        subscription = dispatcher.subscribe("\$memphis_sdk_clients_updates", messageHandler())

    }

    private fun messageHandler() = MessageHandler {
        runBlocking {
            logger.debug { "Received config update" }
            val update = Json.decodeFromString<ConfigurationsUpdate>(String(it.data))

            mutex.withLock {
                when (update.type) {
                    "send_notification" -> clusterConfigurations[update.type] = update.update
                    "schemaverse_to_dls" -> stationSchemaverseToDls[update.stationName] = update.update
                    "remove_station" -> {
                        memphis.stationUpdateManager.removeSchemaUpdateListener(update.stationName)
                        memphis.functionUpdateManager.removeFunctionUpdates(update.stationName)
                        memphis.removeAllConsumers(update.stationName)
                        memphis.removeProducer(update.stationName)
                    }
                    else -> logger.error { "Unrecognized update type: '${update.type}'" }
                }
            }
        }
    }


    suspend fun setClusterConfig(config: String, value: Boolean) {
        mutex.withLock {
            clusterConfigurations[config] = value
        }
    }

    suspend fun setStationSchemaverseToDls(stationName: String, value: Boolean) {
        mutex.withLock {
            stationSchemaverseToDls[stationName] = value
        }
    }

    suspend fun sendMessageToDls(stationName: String): Boolean = mutex.withLock {
        stationSchemaverseToDls[stationName]!!
    }

    suspend fun sendNotification(): Boolean = mutex.withLock {
        clusterConfigurations["send_notification"]!!
    }
}