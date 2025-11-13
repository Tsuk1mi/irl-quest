package com.irlquest.app.data.repository

import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.shared.models.*
import timber.log.Timber

/**
 * Android wrapper для ML Repository из shared модуля
 */
class MLRepository : BaseKmpRepository() {
    private val sharedRepo = SharedRepositoryProvider.mlRepository

    suspend fun generateQuest(todoText: String, context: String? = null): MLQuestGenerationResponse {
        val token = requireToken()
        val request = MLQuestGenerationRequest(
            todoText = todoText,
            context = context,
            userLevel = null, // ML auto-определяет сложность
            tagsOverride = null
        )
        return try {
            sharedRepo.generateQuest(request, token)
        } catch (e: Exception) {
            Timber.e(e, "MLRepository: failed to generate quest")
            throw Exception("Не удалось сгенерировать квест: ${e.message}")
        }
    }

    suspend fun requestVerification(
        questId: Int,
        questTitle: String,
        questDescription: String?,
        userLevel: Int? = null
    ): MLVerificationResponse {
        val token = requireToken()
        val request = MLVerificationRequest(
            questId = questId,
            questTitle = questTitle,
            questDescription = questDescription,
            userLevel = userLevel
        )
        return try {
            sharedRepo.requestVerification(request, token)
        } catch (e: Exception) {
            Timber.e(e, "MLRepository: failed to request verification")
            throw Exception("Не удалось запросить верификацию: ${e.message}")
        }
    }

    suspend fun submitQuiz(questId: Int, answers: List<Int>): QuizSubmitResponse {
        val token = requireToken()
        val request = QuizSubmitRequest(
            questId = questId,
            answers = answers
        )
        return try {
            sharedRepo.submitQuiz(request, token)
        } catch (e: Exception) {
            Timber.e(e, "MLRepository: failed to submit quiz")
            throw Exception("Не удалось отправить тест: ${e.message}")
        }
    }

    suspend fun submitPhoto(
        questId: Int,
        imageBase64: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): PhotoVerificationResponse {
        val token = requireToken()
        val request = PhotoVerificationRequest(
            questId = questId,
            imageBase64 = imageBase64,
            latitude = latitude,
            longitude = longitude
        )
        return try {
            sharedRepo.submitPhoto(request, token)
        } catch (e: Exception) {
            Timber.e(e, "MLRepository: failed to submit photo")
            throw Exception("Не удалось отправить фото: ${e.message}")
        }
    }
}

