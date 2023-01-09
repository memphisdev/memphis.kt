package dev.memphis.sdk.producer

import dev.memphis.sdk.Headers
import kotlin.time.Duration.Companion.seconds

interface Producer {
    val name: String
    val stationName: String

    fun produceAsync(message: ByteArray, options: (ProduceOptions.() -> Unit)? = null)

    suspend fun produce(message: ByteArray, options: (ProduceOptions.() -> Unit)? = null)

    suspend fun destroy()

    class ProduceOptions {
        var ackWait = 15.seconds
        var headers = Headers()
        var messageId: String? = null
    }

    class Options {
        var genUniqueSuffix = false
    }
}