package dev.memphis

import dev.memphis.sdk.Memphis
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val memphis = Memphis.connect("<memphis-host>", "<application type username>", "<broker-token>")

        memphis.createStation("<station-name>") {
            schemaName = "<schema-name>"
            sendPoisonMsgToDls = true
        }
    }
}