package com.irlquest.app.data.repository

import com.irlquest.app.data.network.RetrofitClient
import com.irlquest.app.data.network.dto.CreateTaskRequest
import com.irlquest.app.data.network.dto.TaskDto
import com.irlquest.app.data.network.dto.UpdateTaskRequest
import com.irlquest.app.data.network.dto.RagClassifyRequest
import com.irlquest.app.data.network.dto.RagClassifyResponse
import com.irlquest.app.data.network.dto.RagQuestGenerationRequest
import com.irlquest.app.data.network.dto.RagQuestGenerationResponse
import com.irlquest.app.data.network.dto.CreateQuestRequest
import com.irlquest.app.data.network.dto.QuestDto

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

    suspend fun updateTask(id: Int, title: String? = null, description: String? = null, completed: Boolean? = null, priority: String? = null, difficulty: Int? = null, tags: List<String>? = null): TaskDto {
        // UpdateTaskRequest содержит подходящие поля
        val status = when (completed) {
            true -> "completed"
            false -> "pending"
            null -> null
        }
        return api.updateTask(id, UpdateTaskRequest(title = title, description = description, status = status, priority = priority, difficulty = difficulty, tags = tags)).body()!!
    }

    suspend fun deleteTask(id: Int) {
        api.deleteTask(id)
    }

    // RAG / ML integration
    suspend fun classifyTask(taskText: String, context: String? = null, userLevel: Int? = null): RagClassifyResponse {
        val req = RagClassifyRequest(taskText = taskText, context = context, userLevel = userLevel)
        return api.classifyTask(req).body()!!
    }

    suspend fun generateQuestFromTask(title: String, description: String?, priority: String, estimatedDuration: Int?, difficulty: Int, tags: List<String>): RagQuestGenerationResponse {
        val req = RagQuestGenerationRequest(todoText = title, context = description, difficultyPreference = difficulty, userLevel = null, tagsOverride = if (tags.isEmpty()) null else tags)
        return api.generateQuestRag(req).body()!!
    }

    // Сохранение сгенерированного квеста на сервер как полноценный Quest
    suspend fun createQuestFromGenerated(gen: RagQuestGenerationResponse): QuestDto {
        // Преобразуем генерированные задачи в CreateTaskRequest
        val createTasks = gen.tasks.map { t ->
            CreateTaskRequest(
                title = t.title,
                description = t.description,
                priority = "medium",
                experienceReward = t.experienceReward,
                estimatedDuration = t.estimatedDuration,
                difficulty = t.difficulty,
                questId = null,
                deadline = null,
                tags = emptyList()
            )
        }

        val request = CreateQuestRequest(
            title = gen.title,
            description = gen.description,
            experienceReward = gen.rewardExperience,
            estimatedTime = gen.estimatedTime,
            difficulty = gen.difficulty,
            priority = 2,
            theme = gen.tags.firstOrNull() ?: "",
            tasks = createTasks
        )

        return api.createQuest(request).body()!!
    }
}
