package dev.memphis.sdk

import java.nio.charset.Charset

class MemphisError(message: String, cause: Throwable? = null) :
    RuntimeException(message.replace("nats", "memphis"), cause) {
    constructor(message: ByteArray) : this(message.toString(Charset.defaultCharset()))
}