package com.irlquest.shared.repository

import com.irlquest.shared.models.*
import com.irlquest.shared.network.ApiClient

class TaskRepository(
    private val apiClient: ApiClient
) {
    suspend fun getTasks(token: String? = null): List<TaskDto> {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return apiClient.get("/tasks", headers)
    }
    
    suspend fun createTask(request: CreateTaskRequest, token: String): TaskDto {
        return apiClient.postWithBody(
            "/tasks",
            request,
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
    
    suspend fun completeTask(id: Int, token: String): TaskDto {
        return apiClient.post(
            "/tasks/$id/complete",
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
}

