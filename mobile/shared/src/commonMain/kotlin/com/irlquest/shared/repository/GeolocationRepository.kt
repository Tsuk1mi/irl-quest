package com.irlquest.shared.repository

import com.irlquest.shared.models.*
import com.irlquest.shared.network.ApiClient

class GeolocationRepository(
    private val apiClient: ApiClient
) {
    suspend fun createGeoZone(request: CreateGeoZoneRequest, token: String): GeoZone {
        return apiClient.postWithBody(
            "/geo/zones",
            request,
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
    
    suspend fun checkLocation(request: CheckLocationRequest, token: String): LocationCheckResponse {
        return apiClient.postWithBody(
            "/geo/check",
            request,
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
}

