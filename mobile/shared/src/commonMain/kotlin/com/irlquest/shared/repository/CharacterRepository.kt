package com.irlquest.shared.repository

import com.irlquest.shared.models.*
import com.irlquest.shared.network.ApiClient

class CharacterRepository(
    private val apiClient: ApiClient
) {
    suspend fun getCharacterProfile(token: String): CharacterProfile {
        return apiClient.get(
            "/character/profile",
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
    
    suspend fun selectClassRace(request: SelectClassRaceRequest, token: String) {
        apiClient.post<Unit>(
            "/character/select",
            body = request,
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
    
    suspend fun increaseStat(request: IncreaseStatRequest, token: String) {
        apiClient.postWithBody<Unit, IncreaseStatRequest>(
            "/character/increase-stat",
            request,
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
    
    suspend fun levelUp(token: String): LevelUpResult {
        return apiClient.post<LevelUpResult>(
            "/character/level-up",
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
    
    suspend fun getAvailableClasses(token: String? = null): List<ClassInfo> {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return apiClient.get("/character/classes", headers)
    }
    
    suspend fun getAvailableRaces(token: String? = null): List<RaceInfo> {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return apiClient.get("/character/races", headers)
    }
}

