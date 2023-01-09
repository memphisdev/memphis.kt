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

    // Consume Messages and DLS Messages in one loop
    suspend fun consume(): Flow<Message>

    // Only fetch messages
    suspend fun subscribeMessages(): Flow<Message>

    // Only subscribe to DLS messages
    suspend fun subscribeDls(): Flow<Message>

    fun stopConsuming()

    suspend fun destroy()

    class Options {
        var consumerGroup: String? = null
        var pullInterval: Duration = 1.seconds
        var batchSize: Int = 10
        var batchMaxTimeToWait: Duration = 5.seconds
        var maxAckTime: Duration = 30.seconds
        var maxMsgDeliveries: Int = 10
        var genUniqueSuffix: Boolean = false
    }
}