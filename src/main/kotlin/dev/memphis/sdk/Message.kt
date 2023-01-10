package dev.memphis.sdk

import io.nats.client.Message
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class Message internal constructor(
    private val message: Message,
    private val memphis: Memphis
) {
    val data: ByteArray = message.data
    val headers: Map<String, String>
        get() {
            val headers = mutableMapOf<String, String>()
            message.headers.forEach { t, u -> headers[t] = u.first() }
            return headers
        }

    fun ack() {
        if (message.isJetStream) {
            message.ackSync(30.seconds.toJavaDuration())
        } else {
            val pmId = headers["${'$'}memphis_pm_id"]
            val sequence = headers["\$memphis_pm_sequence"]

            val buf = buildJsonObject {
                put("id", pmId)
                put("sequence", sequence)
            }

            memphis.brokerConnection.publish("${'$'}memphis_pm_acks", buf.toString().toByteArray())
        }
    }
}
