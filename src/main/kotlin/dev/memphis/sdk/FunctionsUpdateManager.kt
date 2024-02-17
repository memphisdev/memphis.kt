package dev.memphis.sdk

import io.nats.client.Subscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class FunctionsUpdateManager(
    val memphis: Memphis
) {
    private val mutex = Mutex()
    private val subs: MutableMap<String, Subscription> = mutableMapOf()
    suspend fun listenToFunctionUpdates(stationName: String, internalFunctions: Map<Int, Int>) {
        mutex.withLock {
            "\$memphis_functions_updates_${stationName.toInternalName()}".let { name ->
                subs[stationName.toInternalName()] = memphis.brokerDispatch.subscribe(name) { msg ->
                    Json.decodeFromString<FunctionUpdate>(String(msg.data)).let { update ->
                        memphis.updatePartitionFunction(name, update)
                    }
                }
            }
        }
    }

    suspend fun removeFunctionUpdates(stationName: String) {
        mutex.withLock {
            stationName.toInternalName().let { station ->
                subs[station]?.let {
                    memphis.brokerDispatch.unsubscribe(it)
                    subs.remove(station)
                }
            }
        }
    }

}

@Serializable
data class FunctionUpdate(
    @SerialName("partitions_functions") val functions: Map<Int, Int>
)