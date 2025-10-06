package com.irlquest.app.data.repository

import com.irlquest.app.data.network.RetrofitClient
import com.irlquest.app.data.network.dto.CreateTaskRequest
import com.irlquest.app.data.network.dto.TaskDto
import com.irlquest.app.data.network.dto.UpdateTaskRequest

class TaskRepository {
    private val api = RetrofitClient.apiService

    suspend fun listTasks(): List<TaskDto> {
        return api.getTasks().body() ?: emptyList()
    }

    suspend fun getTask(id: Int): TaskDto? {
        return api.getTask(id).body()
    }

    suspend fun createTask(title: String, description: String?): TaskDto {
        // CreateTaskRequest требует множество полей — заполним разумными значениями по умолчанию
        val request = CreateTaskRequest(
            title = title,
            description = description ?: "",
            priority = "medium",
            experienceReward = 0,
            estimatedDuration = null,
            difficulty = 1,
            questId = null,
            deadline = null,
            tags = emptyList()
        )
        return api.createTask(request).body()!!
    }

    suspend fun updateTask(id: Int, title: String? = null, description: String? = null, completed: Boolean? = null): TaskDto {
        // UpdateTaskRequest не содержит поля completed, зато есть status
        val status = when (completed) {
            true -> "completed"
            false -> "pending"
            null -> null
        }
        return api.updateTask(id, UpdateTaskRequest(title = title, description = description, status = status)).body()!!
    }

    suspend fun deleteTask(id: Int) {
        api.deleteTask(id)
    }
}
