package dev.memphis.sdk.schemas

internal abstract class Schema(
    val name: String
) {
    abstract fun validateMessage(msg: ByteArray): ByteArray
}
