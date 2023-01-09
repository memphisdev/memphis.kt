package dev.memphis.sdk.schemas

import dev.memphis.sdk.MemphisError
import dev.memphis.sdk.resources.SchemaVersion
import java.nio.charset.Charset
import net.pwall.json.schema.JSONSchema

internal class JsonSchema(
    name: String,
    activeVersion: SchemaVersion
) : Schema(name) {
    private val schema = JSONSchema.parse(activeVersion.content)

    override fun validateMessage(msg: ByteArray): ByteArray {
        val validation = try {
            schema.validateBasic(msg.toString(Charset.defaultCharset()))
        } catch (e: Exception) {
            throw MemphisError("Message does not compile to JSON schema", e)
        }
        if (!validation.valid) {
            val errs = validation.errors?.joinToString("\n") { "${it.error} - ${it.instanceLocation}" }
            throw MemphisError("Message does not compile to JSON schema:\n$errs")
        }
        return msg
    }
}