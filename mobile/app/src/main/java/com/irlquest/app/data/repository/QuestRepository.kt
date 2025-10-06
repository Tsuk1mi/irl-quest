package com.irlquest.app.data.repository

import com.irlquest.app.data.network.RetrofitClient
import com.irlquest.app.data.network.dto.CreateQuestRequest
import com.irlquest.app.data.network.dto.QuestDto
import com.irlquest.app.data.network.dto.UpdateQuestRequest

class QuestRepository {
    private val api = RetrofitClient.apiService

    suspend fun listQuests(): List<QuestDto> {
        return api.getQuests().body() ?: emptyList()
    }

    suspend fun createQuest(title: String, description: String?, difficulty: Int = 1): QuestDto {
        // CreateQuestRequest требует несколько обязательных полей — заполним разумными значениями по умолчанию
        val request = CreateQuestRequest(
            title = title,
            description = description ?: "",
            experienceReward = 0,
            estimatedTime = 0,
            difficulty = difficulty,
            priority = 2,
            theme = "",
            tasks = emptyList()
        )
        return api.createQuest(request).body()!!
    }

    suspend fun getQuest(id: Int): QuestDto? {
        return api.getQuest(id).body()
    }

    suspend fun updateQuest(id: Int, update: UpdateQuestRequest): QuestDto? {
        return api.updateQuest(id, update).body()
    }
}
