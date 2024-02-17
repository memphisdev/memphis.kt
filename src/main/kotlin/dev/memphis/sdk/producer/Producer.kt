package dev.memphis.sdk.producer

import dev.memphis.sdk.Headers
import kotlin.time.Duration.Companion.seconds
import dev.memphis.sdk.MemphisError

interface Producer {
    val name: String
    val stationName: String
    val username:String
    val applicationId:String

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

    fun updatePartitionsFunctions(functions:Map<Int, Int>)
    /**
     * Destroy the producer.
     */
    suspend fun destroy()
    val partitionsFunctions: Map<Int, Int>

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

        /**
         * Represents the partition number for a message in a stream.
         *
         * using a -1 will result in a round robin selection of partitions with each call
         * @property partition The partition number. Default value is -1.
         */
        var partition: Int = -1
    }

    class Options {
        /**
         * Whether to generate a unique suffix for the consumer name.
         */
        var genUniqueSuffix = false
    }
}