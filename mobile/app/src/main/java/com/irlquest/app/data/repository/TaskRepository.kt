package com.irlquest.app.data.repository

import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.app.data.network.dto.CreateQuestRequest
import com.irlquest.app.data.network.dto.CreateTaskRequest
import com.irlquest.app.data.network.dto.QuestDto
import com.irlquest.app.data.network.dto.RagClassifyRequest
import com.irlquest.app.data.network.dto.RagClassifyResponse
import com.irlquest.app.data.network.dto.RagQuestGenerationRequest
import com.irlquest.app.data.network.dto.RagQuestGenerationResponse
import com.irlquest.app.data.network.dto.TaskDto
import com.irlquest.app.data.network.dto.UpdateTaskRequest
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import timber.log.Timber

class TaskRepository : BaseKmpRepository() {
    private val apiClient = SharedRepositoryProvider.apiClient

    suspend fun listTasks(): List<TaskDto> {
        val token = currentToken()
        return try {
            apiClient.get(
                path = "/tasks",
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Failed to load tasks: ${e.response.status.value}")
        } catch (e: ServerResponseException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Server error ${e.response.status.value} while loading tasks")
        }
    }

    suspend fun getTask(id: Int): TaskDto? {
        return listTasks().find { it.id == id }
    }

    suspend fun createTask(title: String, description: String?): TaskDto {
        val token = requireToken()
        val request = CreateTaskRequest(
            title = title,
            description = description,
            priority = "medium",
            experienceReward = 10,
            estimatedDuration = null,
            difficulty = 1,
            questId = null,
            deadline = null,
            tags = null
        )
        return createTask(request, token)
    }

    suspend fun createTaskForQuest(request: CreateTaskRequest): TaskDto {
        val token = requireToken()
        return createTask(request, token)
    }

    private suspend fun createTask(request: CreateTaskRequest, token: String): TaskDto {
        return try {
            apiClient.postWithBody(
                path = "/tasks",
                body = request,
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Failed to create task: ${e.response.status.value}")
        } catch (e: ServerResponseException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Server error ${e.response.status.value} while creating task")
        }
    }

    suspend fun updateTask(
        id: Int,
        @Suppress("UNUSED_PARAMETER") title: String? = null,
        @Suppress("UNUSED_PARAMETER") description: String? = null,
        completed: Boolean? = null,
        @Suppress("UNUSED_PARAMETER") priority: String? = null,
        @Suppress("UNUSED_PARAMETER") difficulty: Int? = null,
        @Suppress("UNUSED_PARAMETER") tags: List<String>? = null
    ): TaskDto {
        if (completed == true) {
            val token = requireToken()
            return try {
                apiClient.post(
                    path = "/tasks/$id/complete",
                    headers = authHeaders(token)
                )
            } catch (e: ClientRequestException) {
                val message = runCatching { e.response.bodyAsText() }.getOrNull()
                throw Exception(message ?: "Failed to complete task: ${e.response.status.value}")
            }
        }
        Timber.w("TaskRepository.updateTask: server does not support partial updates, returning cached task")
        return getTask(id) ?: throw Exception("Task not found")
    }

    suspend fun deleteTask(id: Int) {
        throw UnsupportedOperationException("Server does not support task deletion. Tasks can only be completed.")
    }

    suspend fun classifyTask(taskText: String, context: String? = null, userLevel: Int? = null): RagClassifyResponse {
        val token = currentToken()
        val request = RagClassifyRequest(taskText = taskText, context = context, userLevel = userLevel)
        return apiClient.postWithBody(
            path = "/rag/classify_task",
            body = request,
            headers = authHeaders(token)
        )
    }

    suspend fun generateQuestFromTask(
        title: String,
        description: String?,
        @Suppress("UNUSED_PARAMETER") priority: String,
        @Suppress("UNUSED_PARAMETER") estimatedDuration: Int?,
        difficulty: Int,
        tags: List<String>
    ): RagQuestGenerationResponse {
        val token = currentToken()
        val request = RagQuestGenerationRequest(
            todoText = title,
            context = description,
            difficultyPreference = difficulty,
            userLevel = null,
            tagsOverride = tags.takeIf { it.isNotEmpty() }
        )
        return apiClient.postWithBody(
            path = "/rag/generate_quest",
            body = request,
            headers = authHeaders(token)
        )
    }

    suspend fun createQuestFromGenerated(gen: RagQuestGenerationResponse): QuestDto {
        val token = requireToken()
        val request = CreateQuestRequest(
            title = gen.title,
            description = gen.description,
            difficulty = gen.difficulty,
            status = "active",
            priority = "medium",
            rewardExperience = gen.rewardExperience,
            rewardDescription = "Заверши этот сгенерированный квест!",
            questType = "generated",
            tags = gen.tags,
            isPublic = false
        )
        return apiClient.postWithBody(
            path = "/quests",
            body = request,
            headers = authHeaders(token)
        )
    }
}

