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

    suspend fun createTask(title: String, description: String?): TaskDto {
        return api.createTask(CreateTaskRequest(title = title, description = description)).body()!!
    }

    suspend fun updateTask(id: Int, title: String? = null, description: String? = null, completed: Boolean? = null): TaskDto {
        return api.updateTask(id, UpdateTaskRequest(title = title, description = description, completed = completed)).body()!!
    }

    suspend fun deleteTask(id: Int) {
        api.deleteTask(id)
    }
}
