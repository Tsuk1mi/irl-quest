package com.irlquest.app.data.repository

import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.app.data.network.dto.CoopMissionDto
import com.irlquest.app.data.network.dto.CreateCoopMissionRequest
import com.irlquest.app.data.network.dto.CreateGuildRequest
import com.irlquest.app.data.network.dto.GuildDto
import com.irlquest.app.data.network.dto.GuildMemberDto
import com.irlquest.app.data.network.dto.JoinMissionRequest
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import timber.log.Timber

class MultiplayerRepository : BaseKmpRepository() {
    private val apiClient = SharedRepositoryProvider.apiClient

    suspend fun getGuilds(): List<GuildDto> {
        val token = currentToken()
        return try {
            apiClient.get(
                path = "/guilds",
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Timber.w("MultiplayerRepository: guilds endpoint not implemented on server, returning empty list")
                emptyList()
            } else {
                val message = runCatching { e.response.bodyAsText() }.getOrNull()
                throw Exception(message ?: "Failed to load guilds: ${e.response.status.value}")
            }
        } catch (e: Exception) {
            Timber.e(e, "MultiplayerRepository: error loading guilds")
            emptyList()
        }
    }

    suspend fun createGuild(name: String, description: String?, maxMembers: Int?): GuildDto {
        val token = requireToken()
        val request = CreateGuildRequest(name = name, description = description, maxMembers = maxMembers)
        return try {
            apiClient.postWithBody(
                path = "/guilds",
                body = request,
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Timber.w("MultiplayerRepository: guilds endpoint not implemented")
                throw Exception("⚠️ Функция гильдий пока недоступна. Ожидайте обновление сервера.")
            } else {
                val message = runCatching { e.response.bodyAsText() }.getOrNull()
                throw Exception(message ?: "Failed to create guild: ${e.response.status.value}")
            }
        } catch (e: Exception) {
            Timber.e(e, "MultiplayerRepository: createGuild error")
            throw Exception("⚠️ Функция гильдий пока недоступна. Ожидайте обновление сервера.")
        }
    }

    suspend fun getGuild(id: Int): GuildDto? {
        val token = currentToken()
        return apiClient.get(
            path = "/guilds/$id",
            headers = authHeaders(token)
        )
    }

    suspend fun getGuildMembers(id: Int): List<GuildMemberDto> {
        val token = currentToken()
        return apiClient.get(
            path = "/guilds/$id/members",
            headers = authHeaders(token)
        )
    }

    suspend fun joinGuild(id: Int): Boolean {
        val token = requireToken()
        apiClient.post<Unit>(
            path = "/guilds/$id/join",
            headers = authHeaders(token)
        )
        return true
    }

    suspend fun leaveGuild(id: Int): Boolean {
        val token = requireToken()
        apiClient.post<Unit>(
            path = "/guilds/$id/leave",
            headers = authHeaders(token)
        )
        return true
    }

    suspend fun getCoopMissions(): List<CoopMissionDto> {
        val token = currentToken()
        return try {
            apiClient.get(
                path = "/coop/missions",
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Timber.w("MultiplayerRepository: coop missions endpoint not implemented on server")
                emptyList()
            } else {
                val message = runCatching { e.response.bodyAsText() }.getOrNull()
                throw Exception(message ?: "Failed to load coop missions: ${e.response.status.value}")
            }
        } catch (e: Exception) {
            Timber.e(e, "MultiplayerRepository: error loading coop missions")
            emptyList()
        }
    }

    suspend fun createCoopMission(questId: Int, maxPartySize: Int, isPublic: Boolean): CoopMissionDto {
        val token = requireToken()
        val request = CreateCoopMissionRequest(
            questId = questId,
            maxPartySize = maxPartySize,
            isPublic = isPublic
        )
        return try {
            apiClient.postWithBody(
                path = "/coop/missions",
                body = request,
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                throw Exception("Функция кооперативных миссий пока недоступна. Ожидайте обновление сервера.")
            } else {
                val message = runCatching { e.response.bodyAsText() }.getOrNull()
                throw Exception(message ?: "Failed to create coop mission: ${e.response.status.value}")
            }
        }
    }

    suspend fun getCoopMission(id: Int): CoopMissionDto? {
        val token = currentToken()
        return apiClient.get(
            path = "/coop/missions/$id",
            headers = authHeaders(token)
        )
    }

    suspend fun joinCoopMission(id: Int, preferredRole: String): Boolean {
        val token = requireToken()
        val request = JoinMissionRequest(missionId = id, preferredRole = preferredRole)
        apiClient.postWithBody<Unit, JoinMissionRequest>(
            path = "/coop/missions/join",
            body = request,
            headers = authHeaders(token)
        )
        return true
    }

    suspend fun leaveCoopMission(id: Int): Boolean {
        val token = requireToken()
        apiClient.post<Unit>(
            path = "/coop/missions/$id/leave",
            headers = authHeaders(token)
        )
        return true
    }
}


