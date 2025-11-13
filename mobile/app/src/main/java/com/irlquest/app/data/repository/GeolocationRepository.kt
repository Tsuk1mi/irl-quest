package com.irlquest.app.data.repository

import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.app.data.network.dto.CheckLocationRequest
import com.irlquest.app.data.network.dto.ConsentRequest
import com.irlquest.app.data.network.dto.CreateGeoZoneRequest
import com.irlquest.app.data.network.dto.GeoZone
import com.irlquest.app.data.network.dto.ImageVerificationResponse
import com.irlquest.app.data.network.dto.LocationCheckResponse
import com.irlquest.app.data.network.dto.UploadImageRequest

class GeolocationRepository : BaseKmpRepository() {
    private val apiClient = SharedRepositoryProvider.apiClient

    suspend fun createGeoZone(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        zoneType: String
    ): GeoZone {
        val token = requireToken()
        val request = CreateGeoZoneRequest(
            name = name,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            zoneType = zoneType
        )
        return apiClient.postWithBody(
            path = "/geo/zones",
            body = request,
            headers = authHeaders(token)
        )
    }

    suspend fun checkLocation(latitude: Double, longitude: Double, questId: Int? = null): LocationCheckResponse {
        val token = requireToken()
        val request = CheckLocationRequest(
            latitude = latitude,
            longitude = longitude,
            questId = questId
        )
        return apiClient.postWithBody(
            path = "/geo/check",
            body = request,
            headers = authHeaders(token)
        )
    }

    suspend fun uploadVerificationImage(
        questId: Int,
        imageData: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): ImageVerificationResponse {
        val token = requireToken()
        val request = UploadImageRequest(
            questId = questId,
            imageData = imageData,
            latitude = latitude,
            longitude = longitude
        )
        return apiClient.postWithBody(
            path = "/images/verify",
            body = request,
            headers = authHeaders(token)
        )
    }

    suspend fun giveConsent(
        cameraConsent: Boolean,
        locationConsent: Boolean,
        dataProcessingConsent: Boolean
    ) {
        val token = requireToken()
        val request = ConsentRequest(
            cameraConsent = cameraConsent,
            locationConsent = locationConsent,
            dataProcessingConsent = dataProcessingConsent
        )
        apiClient.postWithBody<Unit, ConsentRequest>(
            path = "/privacy/consent",
            body = request,
            headers = authHeaders(token)
        )
    }
}


