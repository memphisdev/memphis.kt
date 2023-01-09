package dev.memphis.sdk.schemas

import dev.memphis.sdk.MemphisError
import dev.memphis.sdk.resources.SchemaVersion
import graphql.parser.Parser
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.validation.Validator
import java.nio.charset.Charset
import java.util.Locale


internal class GraphQlSchema(
    name: String,
    activeVersion: SchemaVersion
) : Schema(name) {
    private val schemaGenerator = SchemaGenerator()
    private val typeDefinitionRegistry = SchemaParser().parse(activeVersion.content)
    private val schema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, RuntimeWiring.MOCKED_WIRING)

    override fun validateMessage(msg: ByteArray): ByteArray {
        try {
            val doc = Parser.parse(msg.toString(Charset.defaultCharset()))
            val errors = Validator().validateDocument(schema, doc, Locale.getDefault())
                .map { it.message }

            if (errors.isNotEmpty()) throw MemphisError(errors.joinToString())

        } catch (e: Exception) {
            e.printStackTrace()
            throw MemphisError("GraphQl validation failed", e)
        }

        return msg
    }
}