package dev.memphis

import dev.memphis.sdk.Memphis
import java.nio.charset.Charset
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val memphis = Memphis.connect("<memphis-host>", "<application type username>", Memphis.Password("<user-password>"))

        val consumer = memphis.consumer("<station-name>", "<consumer-name>") {
            genUniqueSuffix = true
        }

        val flow = consumer.consume()

        launch {
            flow.collect {
                println("Received message:")
                println(it.data.toString(Charset.defaultCharset()))
                println(it.headers)
                println()
                it.ack()
            }
        }

        delay(30.seconds)

        consumer.stopConsuming()
        memphis.close()
    }
}