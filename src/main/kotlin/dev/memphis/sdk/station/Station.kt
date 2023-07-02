package dev.memphis.sdk.station

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

enum class StorageType(internal val value: String) {
    DISK("file"),
    MEMORY("memory")
}

enum class RetentionType(internal val value: String) {
    MAX_AGE_SECONDS("message_age_sec"),
    MESSAGES("messages"),
    BYTES("bytes")
}

interface Station {
    /**
     * The name of the station
     */
    val name: String

    /**
     * The retention type of the station, either [RetentionType.MAX_AGE_SECONDS], [RetentionType.MESSAGES] or [RetentionType.BYTES].
     * @see RetentionType
     */
    val retentionType: RetentionType

    /**
     * The retention value of the station, either the number of seconds, messages or bytes depending on the [retentionType].
     */
    val retentionValue: Int

    /**
     * The storage type of the station, either [StorageType.DISK] or [StorageType.MEMORY].
     * @see StorageType
     */
    val storageType: StorageType

    /**
     * The number of replicas of the station.
     */
    val replicas: Int

    /**
     * The idempotency window of the station.
     */
    val idempotencyWindow: Duration

    /**
     * The schema name of the station, or null if the station is not attached to a schema.
     */
    val schemaName: String?

    /**
     * Whether to send poison messages to the DLS or not.
     */
    val sendPoisonMsgToDls: Boolean

    /**
     * Whether to send schema failed messages to the DLS or not.
     */
    val sendSchemaFailedMsgToDls: Boolean

    /**
     * Attach the station to a schema.
     * @param schemaName The name of the schema to attach the station to.
     */
    suspend fun attachSchema(schemaName: String)

    /**
     * Detach the station from its schema.
     */
    suspend fun detachSchema()

    /**
     * Destroy the station.
     */
    suspend fun destroy()

    class Options {
        /**
         * The retention type of the station, either [RetentionType.MAX_AGE_SECONDS], [RetentionType.MESSAGES] or [RetentionType.BYTES], default is [RetentionType.MAX_AGE_SECONDS].
         */
        var retentionType = RetentionType.MAX_AGE_SECONDS

        /**
         * The retention value of the station, either the number of seconds, messages or bytes depending on the [retentionType], default is 604800.
         */
        var retentionValue = 604800

        /**
         * The storage type of the station, either [StorageType.DISK] or [StorageType.MEMORY], default is [StorageType.DISK].
         */
        var storageType = StorageType.DISK

        /**
         * The number of replicas of the station, default is 1.
         */
        var replicas = 1

        /**
         * The idempotency window of the station, default is 2 minutes.
         */
        var idempotencyWindow = 2.minutes

        /**
         * The schema name of the station, or null if the station is not attached to a schema.
         */
        var schemaName: String? = null

        /**
         * Whether to send poison messages to the DLS or not, default is true.
         */
        var sendPoisonMsgToDls = true

        /**
         * Whether to send schema failed messages to the DLS or not, default is true.
         */
        var sendSchemaFailedMsgToDls = true
    }
}