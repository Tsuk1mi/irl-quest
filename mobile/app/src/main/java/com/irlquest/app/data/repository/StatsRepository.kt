package com.irlquest.app.data.repository

import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.app.data.network.dto.DailyStatsDto
import com.irlquest.app.data.network.dto.TotalStatsDto
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import timber.log.Timber

class StatsRepository : BaseKmpRepository() {
    private val apiClient = SharedRepositoryProvider.apiClient

    suspend fun getDailyStats(): List<DailyStatsDto> {
        val token = currentToken()
        return try {
            apiClient.get(
                path = "/stats/daily",
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            Timber.e(e, "StatsRepository.getDailyStats: client error")
            emptyList()
        } catch (e: ServerResponseException) {
            Timber.e(e, "StatsRepository.getDailyStats: server error")
            emptyList()
        }
    }

    suspend fun getTotalStats(): TotalStatsDto? {
        val token = currentToken()
        return try {
            apiClient.get(
                path = "/stats/total",
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            Timber.e(e, "StatsRepository.getTotalStats: client error")
            val body = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(body ?: "Failed to load total stats: ${e.response.status.value}")
        } catch (e: ServerResponseException) {
            Timber.e(e, "StatsRepository.getTotalStats: server error")
            val body = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(body ?: "Server error ${e.response.status.value} while loading total stats")
        }
    }
}


