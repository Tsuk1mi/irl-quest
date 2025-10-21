package com.irlquest.app.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RagStudyRequest(
    @SerialName("input_text") val inputText: String,
    val context: String? = null,
    @SerialName("user_level") val userLevel: Int? = null
)

@Serializable
data class RagStudyResponse(
    val summary: String,
    val highlights: List<String> = emptyList()
)

@Serializable
data class RagGenerateRequest(
    @SerialName("input_text") val inputText: String,
    val context: String? = null,
    @SerialName("num_results") val numResults: Int = 1
)

@Serializable
data class RagGenerateResponse(
    val results: List<String> = emptyList()
)

@Serializable
data class RagQuestGenerationRequest(
    @SerialName("todo_text") val todoText: String,
    val context: String? = null,
    @SerialName("difficulty_preference") val difficultyPreference: Int? = null,
    @SerialName("user_level") val userLevel: Int? = null,
    @SerialName("tags_override") val tagsOverride: List<String>? = null
)

@Serializable
data class RagGeneratedTask(
    val title: String,
    val description: String,
    val difficulty: Int,
    @SerialName("experience_reward") val experienceReward: Int,
    @SerialName("estimated_duration") val estimatedDuration: Int? = null,
    @SerialName("is_boss") val isBoss: Boolean = false
)

@Serializable
data class RagQuestGenerationResponse(
    val title: String,
    val description: String,
    val difficulty: Int,
    @SerialName("reward_experience") val rewardExperience: Int,
    @SerialName("reward_description") val rewardDescription: String,
    val tags: List<String> = emptyList(),
    @SerialName("quest_type") val questType: String = "personal",
    val tasks: List<RagGeneratedTask> = emptyList(),
    @SerialName("story_context") val storyContext: String? = null,
    @SerialName("estimated_time") val estimatedTime: Int? = null
)

@Serializable
data class RagClassifyRequest(
    @SerialName("task_text") val taskText: String,
    val context: String? = null,
    @SerialName("user_level") val userLevel: Int? = null
)

@Serializable
data class RagClassifyResponse(
    val tags: List<String> = emptyList(),
    @SerialName("estimated_difficulty") val estimatedDifficulty: Int = 1,
    @SerialName("exam_tasks") val examTasks: List<RagGeneratedTask> = emptyList()
)

@Serializable
data class RagEnhanceRequest(
    @SerialName("task_text") val taskText: String,
    val context: String? = null,
    @SerialName("user_level") val userLevel: Int? = null
)

@Serializable
data class RagEnhanceResponse(
    @SerialName("enhanced_title") val enhancedTitle: String,
    @SerialName("enhanced_description") val enhancedDescription: String,
    @SerialName("suggested_difficulty") val suggestedDifficulty: Int,
    @SerialName("suggested_experience") val suggestedExperience: Int,
    @SerialName("story_context") val storyContext: String? = null,
    @SerialName("suggested_tags") val suggestedTags: List<String> = emptyList()
)
