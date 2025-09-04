package com.irlquest.app.data.repository

import com.irlquest.app.data.network.RetrofitClient
import com.irlquest.app.data.network.dto.TaskCreateRequest
import com.irlquest.app.data.network.dto.TaskDto
import com.irlquest.app.data.network.dto.TaskUpdateRequest

class TaskRepository {
    private val api = RetrofitClient.apiService

    suspend fun listTasks(): List<TaskDto> {
        return api.listTasks()
    }

    suspend fun createTask(title: String, description: String?): TaskDto {
        return api.createTask(TaskCreateRequest(title = title, description = description))
    }

    suspend fun updateTask(id: Int, title: String? = null, description: String? = null, completed: Boolean? = null): TaskDto {
        return api.updateTask(id, TaskUpdateRequest(title = title, description = description, completed = completed))
    }

    suspend fun deleteTask(id: Int) {
        return api.deleteTask(id)
    }
}
