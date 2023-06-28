package dev.memphis.sdk.consumer

import dev.memphis.sdk.Message
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow

interface Consumer {
    val name: String
    val group: String
    val pullInterval: Duration
    val batchSize: Int
    val batchMaxTimeToWait: Duration
    val maxAckTime: Duration
    val maxMsgDeliveries: Int

    /**
     * Consume Messages and DLS Messages in one loop
     * @return Flow<Message> of Messages and DLS Messages
     */
    suspend fun consume(): Flow<Message>

    /**
     * Only fetch messages from the broker.
     * @return Flow<Message> of Messages only
     */
    suspend fun subscribeMessages(): Flow<Message>

    /**
     * Only fetch DLS messages from the broker.
     * @return Flow<Message> of DLS Messages only
     */
    suspend fun subscribeDls(): Flow<Message>

    /**
     * Stop consuming messages.
     */
    fun stopConsuming()

    /**
     * Destroy the consumer.
     */
    suspend fun destroy()

    class Options {
        /**
         * The name of the consumer group to join. If not specified, the consumer will join the default consumer group.
         */
        var consumerGroup: String? = null

        /**
         * The interval at which the consumer will pull messages from the broker.
         */
        var pullInterval: Duration = 1.seconds

        /**
         * The maximum number of messages to pull from the broker at once.
         */
        var batchSize: Int = 10

        /**
         * The maximum amount of time to wait for the batch to fill up before delivering the messages.
         */
        var batchMaxTimeToWait: Duration = 5.seconds

        /**
         * The maximum amount of time to wait for an ack before redelivering the message.
         */
        var maxAckTime: Duration = 30.seconds

        /**
         * The maximum number of times to redeliver a message before sending it to the DLS.
         */
        var maxMsgDeliveries: Int = 10

        /**
         * Whether to generate a unique suffix for the consumer name.
         */
        var genUniqueSuffix: Boolean = false
    }
}