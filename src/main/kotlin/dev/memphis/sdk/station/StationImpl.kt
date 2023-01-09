package dev.memphis.sdk.station

import dev.memphis.sdk.Lifecycle
import dev.memphis.sdk.Memphis
import kotlin.time.Duration
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class StationImpl(
    private val memphis: Memphis,
    override val name: String,
    override val retentionType: RetentionType,
    override val retentionValue: Int,
    override val storageType: StorageType,
    override val replicas: Int,
    override val idempotencyWindow: Duration,
    private val createWithSchemaName: String?,
    override val sendPoisonMsgToDls: Boolean,
    override val sendSchemaFailedMsgToDls: Boolean
) : Station, Lifecycle {
    override val schemaName: String
        get() {
            return memphis.getStationSchema(name).name
        }

    override suspend fun attachSchema(schemaName: String) {
        memphis.attachSchema(schemaName, name)
    }

    override suspend fun detachSchema() {
        memphis.detachSchema(name)
    }

    override suspend fun destroy() {
        memphis.destroyResource(this)
    }

    override fun getCreationSubject(): String =
        "${'$'}memphis_station_creations"

    override fun getCreationRequest(): JsonObject = buildJsonObject {
        put("name", name)
        put("retention_type", retentionType.value)
        put("retention_value", retentionValue)
        put("storage_type", storageType.value)
        put("replicas", replicas)
        put("idempotency_window_in_ms", idempotencyWindow.inWholeMilliseconds.toInt())
        put("schema_name", createWithSchemaName ?: "")
        putJsonObject("dls_configuration") {
            put("poison", sendPoisonMsgToDls)
            put("schemaverse", sendSchemaFailedMsgToDls)
        }
    }

    override fun getDestructionSubject(): String =
        "${'$'}memphis_station_destructions"

    override fun getDestructionRequest(): JsonObject = buildJsonObject {
        put("station_name", name)
    }
}