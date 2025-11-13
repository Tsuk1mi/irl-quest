package com.irlquest.app.data.repository

import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.app.data.network.dto.AcceptQuestRequest
import com.irlquest.app.data.network.dto.MergeSuggestionsResponse
import com.irlquest.app.data.network.dto.QuestSuggestionResponse

class QuestSuggestionsRepository : BaseKmpRepository() {
    private val apiClient = SharedRepositoryProvider.apiClient

    suspend fun getDailyQuestSuggestion(): QuestSuggestionResponse {
        val token = currentToken()
        return apiClient.get(
            path = "/quests/suggestions/daily",
            headers = authHeaders(token)
        )
    }

    suspend fun getWeeklyQuestSuggestion(): QuestSuggestionResponse {
        val token = currentToken()
        return apiClient.get(
            path = "/quests/suggestions/weekly",
            headers = authHeaders(token)
        )
    }

    suspend fun getMergeSuggestions(): MergeSuggestionsResponse {
        val token = currentToken()
        return apiClient.get(
            path = "/quests/suggestions/merge",
            headers = authHeaders(token)
        )
    }

    suspend fun acceptDailyQuest(title: String, description: String? = null) {
        val token = requireToken()
        val request = AcceptQuestRequest(title = title, description = description)
        apiClient.postWithBody<Unit, AcceptQuestRequest>(
            path = "/quests/suggestions/daily/accept",
            body = request,
            headers = authHeaders(token)
        )
    }
}


