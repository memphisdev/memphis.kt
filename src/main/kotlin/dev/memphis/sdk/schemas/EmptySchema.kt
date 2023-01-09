package dev.memphis.sdk.schemas

internal class EmptySchema : Schema("empty") {
    override fun validateMessage(msg: ByteArray): ByteArray {
        return msg
    }
}