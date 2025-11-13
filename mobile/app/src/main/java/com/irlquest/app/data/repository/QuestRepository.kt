package com.irlquest.app.data.repository

import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.app.data.network.dto.CreateQuestRequest
import com.irlquest.app.data.network.dto.QuestDto
import com.irlquest.app.data.network.dto.UpdateQuestRequest
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import timber.log.Timber

class QuestRepository : BaseKmpRepository() {
    private val apiClient = SharedRepositoryProvider.apiClient

    suspend fun listQuests(): List<QuestDto> {
        val token = currentToken()
        return try {
            apiClient.get(
                path = "/quests",
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            Timber.e(e, "QuestRepository.listQuests: client error")
            val body = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(body ?: "Failed to load quests: ${e.response.status.value}")
        } catch (e: ServerResponseException) {
            Timber.e(e, "QuestRepository.listQuests: server error")
            val body = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(body ?: "Server error ${e.response.status.value} while loading quests")
        }
    }

    suspend fun createQuest(title: String, description: String?, difficulty: Int = 1): QuestDto {
        val token = requireToken()
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

        Timber.d("QuestRepository: Creating quest title='%s' difficulty=%d", title, difficulty)

        return try {
            apiClient.postWithBody(
                path = "/quests",
                body = request,
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            val body = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(body ?: "Failed to create quest: ${e.response.status.value}")
        } catch (e: ServerResponseException) {
            val body = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(body ?: "Server error ${e.response.status.value} while creating quest")
        }
    }

    suspend fun getQuest(id: Int): QuestDto? {
        val token = currentToken()
        return try {
            apiClient.get(
                path = "/quests/$id",
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Timber.w("QuestRepository: quest %d not found", id)
                null
            } else {
                val body = runCatching { e.response.bodyAsText() }.getOrNull()
                throw Exception(body ?: "Failed to load quest: ${e.response.status.value}")
            }
        }
    }

    suspend fun updateQuest(id: Int, update: UpdateQuestRequest): QuestDto {
        val token = requireToken()
        return try {
            apiClient.postWithBody(
                path = "/quests/$id",
                body = update,
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            val body = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(body ?: "Failed to update quest: ${e.response.status.value}")
        } catch (e: ServerResponseException) {
            val body = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(body ?: "Server error ${e.response.status.value} while updating quest")
        }
    }
}

