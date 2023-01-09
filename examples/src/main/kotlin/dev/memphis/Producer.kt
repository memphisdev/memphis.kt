package dev.memphis

import dev.memphis.sdk.Memphis
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val memphis = Memphis.connect("<memphis-host>", "<application type username>", "<broker-token>")

        val producer = memphis.producer("<station-name>", "<producer-name>")

        producer.produce("You have a message!".toByteArray()) {
            headers.put("key", "value")
        }

        producer.destroy()
        memphis.close()
    }
}
