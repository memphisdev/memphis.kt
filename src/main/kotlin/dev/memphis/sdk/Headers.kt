package dev.memphis.sdk

import io.nats.client.impl.Headers

class Headers {
    internal val headers = Headers()

    /**
     * Adds a header to the message.
     * @param key the header key
     * @param value the header value
     * @throws MemphisError if the key starts with `${'$'}memphis`
     */
    fun put(key: String, value: String) {
        validateKey(key)
        putUnchecked(key, value)
    }

    private fun validateKey(key: String) {
        if (key.startsWith("${'$'}memphis")) throw MemphisError("Keys in headers should not start with ${'$'}memphis")
    }

    internal fun putUnchecked(key: String, value: String) = headers.put(key, value)

}