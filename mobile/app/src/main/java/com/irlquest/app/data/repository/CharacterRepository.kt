package com.irlquest.app.data.repository

import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.app.data.network.dto.CharacterProfile
import com.irlquest.app.data.network.dto.ClassInfo
import com.irlquest.app.data.network.dto.IncreaseStatRequest
import com.irlquest.app.data.network.dto.LevelUpResult
import com.irlquest.app.data.network.dto.RaceInfo
import com.irlquest.app.data.network.dto.SelectClassRaceRequest
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText

class CharacterRepository : BaseKmpRepository() {
    private val apiClient = SharedRepositoryProvider.apiClient

    suspend fun getCharacterProfile(): CharacterProfile {
        val token = requireToken()
        return apiClient.get(
            path = "/character/profile",
            headers = authHeaders(token)
        )
    }

    suspend fun selectClassRace(characterClass: String, race: String) {
        val token = requireToken()
        val request = SelectClassRaceRequest(characterClass = characterClass, race = race)
        try {
            apiClient.postWithBody<Unit, SelectClassRaceRequest>(
                path = "/character/select",
                body = request,
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Failed to select class/race: ${e.response.status.value}")
        } catch (e: ServerResponseException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Server error ${e.response.status.value} while selecting class/race")
        }
    }

    suspend fun increaseStat(statName: String, amount: Int = 1) {
        val token = requireToken()
        val request = IncreaseStatRequest(statName = statName, amount = amount)
        try {
            apiClient.postWithBody<Unit, IncreaseStatRequest>(
                path = "/character/increase-stat",
                body = request,
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Failed to increase stat: ${e.response.status.value}")
        }
    }

    suspend fun levelUp(): LevelUpResult {
        val token = requireToken()
        return apiClient.post(
            path = "/character/level-up",
            headers = authHeaders(token)
        )
    }

    suspend fun getAvailableClasses(): List<ClassInfo> {
        val token = currentToken()
        return apiClient.get(
            path = "/character/classes",
            headers = authHeaders(token)
        )
    }

    suspend fun getAvailableRaces(): List<RaceInfo> {
        val token = currentToken()
        return apiClient.get(
            path = "/character/races",
            headers = authHeaders(token)
        )
    }
}


