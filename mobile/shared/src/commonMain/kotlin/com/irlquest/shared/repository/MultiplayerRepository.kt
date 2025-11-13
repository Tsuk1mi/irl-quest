package com.irlquest.shared.repository

import com.irlquest.shared.models.*
import com.irlquest.shared.network.ApiClient

class MultiplayerRepository(
    private val apiClient: ApiClient
) {
    // Guilds
    suspend fun getGuilds(token: String? = null): List<GuildDto> {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return apiClient.get("/guilds", headers)
    }
    
    suspend fun createGuild(request: CreateGuildRequest, token: String): GuildDto {
        return apiClient.postWithBody(
            "/guilds",
            request,
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
    
    suspend fun getGuild(id: Int, token: String? = null): GuildDto {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return apiClient.get("/guilds/$id", headers)
    }
    
    suspend fun getGuildMembers(id: Int, token: String? = null): List<GuildMemberDto> {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return apiClient.get("/guilds/$id/members", headers)
    }
    
    suspend fun joinGuild(id: Int, token: String) {
        apiClient.post<Unit>(
            "/guilds/$id/join",
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
    
    suspend fun leaveGuild(id: Int, token: String) {
        apiClient.post<Unit>(
            "/guilds/$id/leave",
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
    
    // Coop Missions
    suspend fun getCoopMissions(token: String? = null): List<CoopMissionDto> {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return apiClient.get("/coop/missions", headers)
    }
    
    suspend fun createCoopMission(request: CreateCoopMissionRequest, token: String): CoopMissionDto {
        return apiClient.postWithBody(
            "/coop/missions",
            request,
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
    
    suspend fun getCoopMission(id: Int, token: String? = null): CoopMissionDto {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return apiClient.get("/coop/missions/$id", headers)
    }
    
    suspend fun joinCoopMission(request: JoinMissionRequest, token: String): CoopMissionDto {
        return apiClient.postWithBody(
            "/coop/missions/join",
            request,
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
    
    suspend fun leaveCoopMission(id: Int, token: String) {
        apiClient.post<Unit>(
            "/coop/missions/$id/leave",
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
}

