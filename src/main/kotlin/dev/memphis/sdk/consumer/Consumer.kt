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
    val startConsumeFromSequence: Int?
    val lastMessages: Int?
    val partitionNumber: Int
    val username:String
    val applicationId:String

    /**
     * Consume Messages and DLS Messages
     *
     * @param partitions an optional list of one or more partitions to consume from.
     * @return Flow<Message> of Messages and DLS Messages
     */
    suspend fun consume(vararg partitions:Int): Flow<Message>

    /**
     * Only fetch messages from the broker.
     * @param partitions an optional list of one or more partitions to consume from.
     * @return Flow<Message> of Messages only
     */
    suspend fun subscribeMessages(vararg partitions:Int): Flow<Message>

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

        /**
         * Nullable variable that represents the starting sequence number for consuming messages from a sequence.
         * The sequence number indicates the position of a message in the sequence of messages.
         * If null, the consumer will start consuming from the beginning of the sequence.
         * Defaults to 1.
         */
        var startConsumeFromSequence: Int? = 1

        /**
         * Nullable variable that represents the last read message index.
         * The lastMessages variable indicates the index of the last message that was read from the consumer.
         * If null, no messages have been read yet.
         * Defaults to -1.
         */
        var lastMessages:Int? = -1

        /**
         * The partition number indicates the specific partition from which messages are consumed.
         * Setting this value to -1 will result in all partitions being used
         *
         * Default value is -1.
         */
        var partitionNumber : Int = -1

    }
}