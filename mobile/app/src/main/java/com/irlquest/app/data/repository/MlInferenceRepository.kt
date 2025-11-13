package com.irlquest.app.data.repository

import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.app.data.network.dto.MlConfig
import com.irlquest.app.data.network.dto.RecommendationsRequest
import com.irlquest.app.data.network.dto.RecommendationsResponse
import com.irlquest.app.data.network.dto.TagsRequest
import com.irlquest.app.data.network.dto.TagsResponse
import com.irlquest.app.data.network.dto.TransformRequest
import com.irlquest.app.data.network.dto.TransformResponse

class MlInferenceRepository : BaseKmpRepository() {
    private val apiClient = SharedRepositoryProvider.apiClient

    suspend fun predictTags(text: String, maxTags: Int = 5): TagsResponse {
        val token = currentToken()
        val request = TagsRequest(text = text, maxTags = maxTags)
        return apiClient.postWithBody(
            path = "/ml/tags",
            body = request,
            headers = authHeaders(token)
        )
    }

    suspend fun transformToQuest(
        title: String,
        description: String? = null,
        difficulty: Int? = null,
        userLevel: Int? = null
    ): TransformResponse {
        val token = currentToken()
        val request = TransformRequest(
            title = title,
            description = description,
            difficulty = difficulty,
            userLevel = userLevel
        )
        return apiClient.postWithBody(
            path = "/ml/transform",
            body = request,
            headers = authHeaders(token)
        )
    }

    suspend fun getRecommendations(userId: Int, limit: Int = 10): RecommendationsResponse {
        val token = currentToken()
        val request = RecommendationsRequest(userId = userId, limit = limit)
        return apiClient.postWithBody(
            path = "/ml/recommendations",
            body = request,
            headers = authHeaders(token)
        )
    }

    suspend fun getMlConfig(): MlConfig {
        val token = currentToken()
        return apiClient.get(
            path = "/ml/config",
            headers = authHeaders(token)
        )
    }
}


