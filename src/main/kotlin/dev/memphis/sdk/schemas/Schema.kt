package dev.memphis.sdk.schemas

import dev.memphis.sdk.MemphisError

internal abstract class Schema(
    /**
     * The name of the schema.
     */
    val name: String
) {
    /**
     * Validates the message and returns the validated message, or throws an exception if the message is invalid.
     * @param msg The message to validate.
     * @return [ByteArray] The validated message.
     * @throws MemphisError if the message is invalid.
     */
    abstract fun validateMessage(msg: ByteArray): ByteArray
}
