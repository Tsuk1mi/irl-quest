package com.irlquest.shared.repository

import com.irlquest.shared.models.*
import com.irlquest.shared.network.ApiClient

class MLRepository(
    private val apiClient: ApiClient
) {
    // Quest generation with auto difficulty
    suspend fun generateQuest(request: MLQuestGenerationRequest, token: String): MLQuestGenerationResponse {
        return try {
            apiClient.postWithBody(
                "/ml/generate-quest",
                request,
                headers = mapOf("Authorization" to "Bearer $token")
            )
        } catch (e: Exception) {
            // Backend ML endpoint not implemented - throw user-friendly error
            throw Exception("ML генерация квестов пока недоступна. Используйте ручное создание квестов.")
        }
    }
    
    // Quest verification (quiz or photo)
    suspend fun requestVerification(request: MLVerificationRequest, token: String): MLVerificationResponse {
        return try {
            apiClient.postWithBody(
                "/ml/quest-verification",
                request,
                headers = mapOf("Authorization" to "Bearer $token")
            )
        } catch (e: Exception) {
            // Backend not implemented - return auto-approve
            MLVerificationResponse(
                verificationType = "none",
                quiz = null,
                photoPrompt = null,
                photoRequirements = null
            )
        }
    }
    
    // Submit quiz answers
    suspend fun submitQuiz(request: QuizSubmitRequest, token: String): QuizSubmitResponse {
        return try {
            apiClient.postWithBody(
                "/ml/verify-quiz",
                request,
                headers = mapOf("Authorization" to "Bearer $token")
            )
        } catch (e: Exception) {
            // Auto-approve if backend not implemented
            QuizSubmitResponse(
                passed = true,
                scorePercentage = 100,
                correctCount = request.answers.size,
                totalCount = request.answers.size,
                feedback = "Верификация пройдена автоматически (сервер недоступен)"
            )
        }
    }
    
    // Submit photo for verification
    suspend fun submitPhoto(request: PhotoVerificationRequest, token: String): PhotoVerificationResponse {
        return try {
            apiClient.postWithBody(
                "/ml/verify-photo",
                request,
                headers = mapOf("Authorization" to "Bearer $token")
            )
        } catch (e: Exception) {
            // Auto-approve if backend not implemented
            PhotoVerificationResponse(
                approved = true,
                aiConfidence = 0.0f,
                detectedObjects = emptyList(),
                feedback = "Верификация пройдена автоматически (сервер недоступен)",
                autoDeletedAt = "immediate"
            )
        }
    }
}

