package dev.memphis.sdk.producer

import dev.memphis.sdk.Headers
import kotlin.time.Duration.Companion.seconds
import dev.memphis.sdk.MemphisError

interface Producer {
    val name: String
    val stationName: String

    /**
     * Produce a message to the broker asynchronously.
     * @param message The message to produce.
     * @param options The options to use when producing the message, see [ProduceOptions].
     * @throws MemphisError if the message is too large or the broker is not available.
     * @see [ProduceOptions]
     */
    fun produceAsync(message: ByteArray, options: (ProduceOptions.() -> Unit)? = null)

    /**
     * Produce a message to the broker.
     * @param message The message to produce.
     * @param options The options to use when producing the message, see [ProduceOptions].
     * @throws MemphisError if the message is too large or the broker is not available.
     * @see [ProduceOptions]
     */
    suspend fun produce(message: ByteArray, options: (ProduceOptions.() -> Unit)? = null)

    /**
     * Produce a message to the broker.
     * @param message The message to produce.
     * @param options The options to use when producing the message, see [ProduceOptions].
     * @throws MemphisError if the message is too large or the broker is not available.
     * @see [ProduceOptions]
     */
    suspend fun produce(message: String, options: (ProduceOptions.() -> Unit)? = null) {
        produce(message.encodeToByteArray(), options)
    }

    /**
     * Destroy the producer.
     */
    suspend fun destroy()

    class ProduceOptions {
        /**
         * The maximum time to wait for an ack from the broker.
         */
        var ackWait = 15.seconds

        /**
         * List of stream headers to add to the message.
         */
        var headers = Headers()

        /**
         * The ID of the message to produce, if not specified a random ID will be generated.
         */
        var messageId: String? = null
    }

    class Options {
        /**
         * Whether to generate a unique suffix for the consumer name.
         */
        var genUniqueSuffix = false
    }
}