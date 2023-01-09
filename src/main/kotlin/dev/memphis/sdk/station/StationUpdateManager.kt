package dev.memphis.sdk.station

import dev.memphis.sdk.MemphisError
import dev.memphis.sdk.resources.SchemaUpdateInit
import io.nats.client.Dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging

internal class StationUpdateManager(
    private val dispatcher: Dispatcher,
    private val scope: CoroutineScope
) {
    private val logger = KotlinLogging.logger {}

    private val stationUpdateSubscriptions = mutableMapOf<String, StationUpdateSubscription>()
    private val mutex = Mutex()

    operator fun get(stationName: String): StationUpdateSubscription = stationUpdateSubscriptions[stationName]
        ?: throw MemphisError("station subscription doesn't exist")

    suspend fun listenToSchemaUpdates(stationName: String) {
        val subs = stationUpdateSubscriptions[stationName]
        if (subs != null) {
            subs.increaseRefCount()
            return
        }

        val stationSub = StationUpdateSubscription()
        scope.launch { stationSub.updatesHandler(mutex) }

        val sub = dispatcher.subscribe("${'$'}memphis_schema_updates_$stationName", stationSub.messageHandler())
        stationSub.startSubscription(sub)

        stationUpdateSubscriptions[stationName] = stationSub
        println("applying $stationName")
    }

    suspend fun removeSchemaUpdateListener(stationName: String) {
        mutex.withLock {
            val sub = stationUpdateSubscriptions[stationName] ?: throw MemphisError("listener doesn't exist")
            sub.decreaseRefCount()
            if (sub.getRefCount() == 0u) {
                sub.teardown()
                stationUpdateSubscriptions.remove(stationName)
            }
        }
    }

    suspend fun applySchema(stationName: String, schemaUpdateInit: SchemaUpdateInit) {
        mutex.withLock {
            val sub = stationUpdateSubscriptions[stationName] ?: throw MemphisError("listener doesn't exist")
            sub.applySchema(schemaUpdateInit)
        }
    }
}