package com.irlquest.app.data.repository

import com.irlquest.app.data.network.RetrofitClient
import com.irlquest.app.data.network.dto.CreateQuestRequest
import com.irlquest.app.data.network.dto.QuestDto
import com.irlquest.app.data.network.dto.UpdateQuestRequest
import timber.log.Timber

class QuestRepository {
    private val api = RetrofitClient.apiService

    suspend fun listQuests(): List<QuestDto> {
        return api.getQuests().body() ?: emptyList()
    }

    suspend fun createQuest(title: String, description: String?, difficulty: Int = 1): QuestDto {
        val xpReward = difficulty * 50
        val request = CreateQuestRequest(
            title = title,
            description = description,
            difficulty = difficulty,
            status = "active",
            priority = "medium",
            rewardExperience = xpReward,
            rewardDescription = "Заверши этот квест для получения наград!",
            questType = "manual",
            isPublic = false
        )
        
        Timber.d("QuestRepository: Creating quest: title='$title', difficulty=$difficulty")
        
        val response = api.createQuest(request)
        
        Timber.d("QuestRepository: Response code=${response.code()}, isSuccessful=${response.isSuccessful()}")
        
        if (!response.isSuccessful()) {
            val errorBody = response.errorBody()?.string()
            Timber.e("QuestRepository: Server error: $errorBody")
            throw Exception("Ошибка сервера ${response.code()}: $errorBody")
        }
        
        val body = response.body()
        if (body == null) {
            Timber.e("QuestRepository: Response body is null despite success code")
            throw Exception("Сервер вернул пустой ответ")
        }
        
        Timber.d("QuestRepository: Quest created successfully, id=${body.id}")
        return body
    }

    suspend fun getQuest(id: Int): QuestDto? {
        return api.getQuest(id).body()
    }

    suspend fun updateQuest(id: Int, update: UpdateQuestRequest): QuestDto? {
        return api.updateQuest(id, update).body()
    }
}
