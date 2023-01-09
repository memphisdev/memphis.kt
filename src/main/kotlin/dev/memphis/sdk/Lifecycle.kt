package dev.memphis.sdk

import io.nats.client.Message
import kotlinx.serialization.json.JsonObject

internal interface Lifecycle : Create, Destroy

internal interface Create {
    fun getCreationSubject(): String
    fun getCreationRequest(): JsonObject
    suspend fun handleCreationResponse(msg: Message) {
        if (msg.data.isEmpty()) return
        throw MemphisError(msg.data)
    }
}

internal interface Destroy {
    fun getDestructionSubject(): String
    fun getDestructionRequest(): JsonObject
}