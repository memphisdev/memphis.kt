package dev.memphis.sdk

import io.nats.client.Message
import kotlinx.serialization.json.JsonObject

internal interface Lifecycle : Create, Destroy

internal interface Create {
    /**
     * The subject to which the creation request will be sent.
     * @return The subject to which the creation request will be sent.
     */
    fun getCreationSubject(): String

    /**
     * The creation request.
     * @return The creation request.
     */
    fun getCreationRequest(): JsonObject

    /**
     * Handles the response to the creation request.
     * @param msg The response message.
     * @throws MemphisError if the response is an error.
     */
    suspend fun handleCreationResponse(msg: Message) {
        if (msg.data.isEmpty()) return
        throw MemphisError(msg.data)
    }
}

internal interface Destroy {
    /**
     * The subject to which the destruction request will be sent.
     * @return The subject to which the destruction request will be sent.
     */
    fun getDestructionSubject(): String

    /**
     * The destruction request.
     * @return The destruction request.
     */
    fun getDestructionRequest(): JsonObject
}