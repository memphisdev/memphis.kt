package dev.memphis.sdk.schemas

import dev.memphis.sdk.Create
import dev.memphis.sdk.Destroy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object SchemaLifecycle {
    class Attach(
        private val schemaName: String,
        private val stationName: String
    ) : Create {
        override fun getCreationSubject(): String =
            "${'$'}memphis_schema_attachments"

        override fun getCreationRequest(): JsonObject = buildJsonObject {
            put("name", schemaName)
            put("station_name", stationName)
        }
    }

    class Detach(private val stationName: String) : Destroy {
        override fun getDestructionSubject(): String =
            "${'$'}memphis_schema_detachments"

        override fun getDestructionRequest(): JsonObject = buildJsonObject {
            put("station_name", stationName)
        }
    }
}