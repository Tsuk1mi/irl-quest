package com.irlquest.shared.repository

import com.irlquest.shared.models.*
import com.irlquest.shared.network.ApiClient

class StatsRepository(
    private val apiClient: ApiClient
) {
    suspend fun getDailyStats(token: String? = null): List<DailyStatsDto> {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return apiClient.get("/stats/daily", headers)
    }
    
    suspend fun getTotalStats(token: String? = null): TotalStatsDto {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return apiClient.get("/stats/total", headers)
    }
}

