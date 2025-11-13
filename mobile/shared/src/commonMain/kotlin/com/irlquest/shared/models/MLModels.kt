package com.irlquest.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ML Quest Generation
@Serializable
data class MLQuestGenerationRequest(
    @SerialName("todo_text") val todoText: String,
    val context: String? = null,
    @SerialName("user_level") val userLevel: Int? = null,
    @SerialName("tags_override") val tagsOverride: List<String>? = null
)

@Serializable
data class MLGeneratedTask(
    val title: String,
    val description: String,
    val difficulty: Int,
    @SerialName("experience_reward") val experienceReward: Int,
    @SerialName("estimated_duration") val estimatedDuration: Int? = null,
    @SerialName("is_boss") val isBoss: Boolean = false
)

@Serializable
data class MLQuestGenerationResponse(
    val title: String,
    val description: String,
    val difficulty: Int,
    @SerialName("reward_experience") val rewardExperience: Int,
    @SerialName("reward_description") val rewardDescription: String,
    val tags: List<String> = emptyList(),
    @SerialName("quest_type") val questType: String = "personal",
    val tasks: List<MLGeneratedTask> = emptyList(),
    @SerialName("story_context") val storyContext: String? = null,
    @SerialName("estimated_time") val estimatedTime: Int? = null
)

// ML Quest Verification
@Serializable
data class MLVerificationRequest(
    @SerialName("quest_id") val questId: Int,
    @SerialName("quest_title") val questTitle: String,
    @SerialName("quest_description") val questDescription: String?,
    @SerialName("user_level") val userLevel: Int? = null
)

@Serializable
data class MLVerificationResponse(
    @SerialName("verification_type") val verificationType: String, // "quiz", "photo", "none"
    val quiz: QuizVerification? = null,
    @SerialName("photo_prompt") val photoPrompt: String? = null,
    @SerialName("photo_requirements") val photoRequirements: List<String>? = null
)

@Serializable
data class QuizVerification(
    val questions: List<QuizQuestion>
)

@Serializable
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    @SerialName("correct_answer_index") val correctAnswerIndex: Int
)

// Quiz submission
@Serializable
data class QuizSubmitRequest(
    @SerialName("quest_id") val questId: Int,
    val answers: List<Int>
)

@Serializable
data class QuizSubmitResponse(
    val passed: Boolean,
    @SerialName("score_percentage") val scorePercentage: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("total_count") val totalCount: Int,
    val feedback: String
)

// Photo verification
@Serializable
data class PhotoVerificationRequest(
    @SerialName("quest_id") val questId: Int,
    @SerialName("image_base64") val imageBase64: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class PhotoVerificationResponse(
    val approved: Boolean,
    @SerialName("ai_confidence") val aiConfidence: Float,
    @SerialName("detected_objects") val detectedObjects: List<String>,
    val feedback: String,
    @SerialName("auto_deleted_at") val autoDeletedAt: String // Время автоудаления фото
)

