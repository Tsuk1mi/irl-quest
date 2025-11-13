package com.irlquest.app.data.repository

import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.app.data.network.dto.ClientConfig
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText

class ConfigRepository : BaseKmpRepository() {
    private val apiClient = SharedRepositoryProvider.apiClient

    suspend fun getClientConfig(): ClientConfig {
        val token = currentToken()
        return try {
            apiClient.get(
                path = "/config",
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Failed to get client config: ${e.response.status.value}")
        } catch (e: ServerResponseException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Server error ${e.response.status.value} while fetching client config")
        }
    }
}


