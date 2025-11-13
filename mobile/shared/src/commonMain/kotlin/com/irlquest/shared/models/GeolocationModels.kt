package com.irlquest.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeoZone(
    val id: Int,
    val name: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    @SerialName("quest_id") val questId: Int? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CreateGeoZoneRequest(
    val name: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    @SerialName("quest_id") val questId: Int? = null
)

@Serializable
data class CheckLocationRequest(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class LocationCheckResponse(
    val inZone: Boolean,
    @SerialName("zone_id") val zoneId: Int? = null,
    @SerialName("zone_name") val zoneName: String? = null,
    val distance: Double? = null
)

