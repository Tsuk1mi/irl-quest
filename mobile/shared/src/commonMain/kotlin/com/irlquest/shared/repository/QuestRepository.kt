package com.irlquest.shared.repository

import com.irlquest.shared.models.*
import com.irlquest.shared.network.ApiClient

class QuestRepository(
    private val apiClient: ApiClient
) {
    suspend fun getQuests(token: String? = null): List<QuestDto> {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return apiClient.get("/quests", headers)
    }
    
    suspend fun getQuest(id: Int, token: String? = null): QuestDto {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return apiClient.get("/quests/$id", headers)
    }
    
    suspend fun createQuest(request: CreateQuestRequest, token: String): QuestDto {
        return apiClient.postWithBody(
            "/quests",
            request,
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
    
    suspend fun updateQuest(id: Int, request: UpdateQuestRequest, token: String): QuestDto {
        return apiClient.postWithBody(
            "/quests/$id",
            request,
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
}

