package dev.memphis.sdk.resources

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DlsMessage(
    @SerialName("_id") val id: String,
    @SerialName("station_name") val stationName: String,
    val producer: ProducerDetails,
    val message: MessagePayloadDls,
    @SerialName("creation_date") val creationDate: Instant
) {
    @Serializable
    data class ProducerDetails(
        val name: String,
        @SerialName("connection_id") val connectionId: String
    )

    @Serializable
    data class MessagePayloadDls(
        @SerialName("time_sent") val timeSent: Instant,
        val size: Int = 0,
        val data: String,
        val headers: Map<String, String>
    )
}