package com.irlquest.app.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Запрос на генерацию квеста из TODO
@Serializable
data class QuestGenerationRequest(
    @SerialName("todo_text") val todoText: String,
    val context: String? = null,
    @SerialName("difficulty_preference") val difficultyPreference: Int? = null,
    @SerialName("theme_preference") val themePreference: String? = null,
    @SerialName("user_level") val userLevel: Int? = null
)

// Ответ с сгенерированным квестом
@Serializable
data class QuestGenerationResponse(
    val title: String,
    val description: String,
    val difficulty: Int,
    @SerialName("reward_experience") val rewardExperience: Int,
    @SerialName("reward_description") val rewardDescription: String,
    val tags: List<String>,
    @SerialName("quest_type") val questType: String,
    val tasks: List<GeneratedTaskDto>,
    @SerialName("story_context") val storyContext: String? = null
)

@Serializable
data class GeneratedTaskDto(
    val title: String,
    val description: String,
    val difficulty: Int,
    @SerialName("experience_reward") val experienceReward: Int,
    @SerialName("estimated_duration") val estimatedDuration: Int? = null
)

// Запрос на улучшение задачи
@Serializable
data class TaskEnhancementRequest(
    @SerialName("task_text") val taskText: String,
    val context: String? = null,
    @SerialName("user_level") val userLevel: Int? = null
)

// Ответ с улучшенной задачей
@Serializable
data class TaskEnhancementResponse(
    @SerialName("enhanced_title") val enhancedTitle: String,
    @SerialName("enhanced_description") val enhancedDescription: String,
    @SerialName("suggested_difficulty") val suggestedDifficulty: Int,
    @SerialName("suggested_experience") val suggestedExperience: Int,
    @SerialName("story_context") val storyContext: String? = null,
    @SerialName("suggested_tags") val suggestedTags: List<String>
)

// Статистика пользователя
@Serializable
data class UserStatsDto(
    val level: Int,
    val experience: Int,
    @SerialName("total_quests") val totalQuests: Int,
    @SerialName("completed_quests") val completedQuests: Int,
    @SerialName("total_tasks") val totalTasks: Int,
    @SerialName("completed_tasks") val completedTasks: Int,
    @SerialName("achievements_count") val achievementsCount: Int
)

// Достижения пользователя
@Serializable
data class AchievementDto(
    val id: Int,
    @SerialName("achievement_type") val achievementType: String,
    @SerialName("achievement_data") val achievementData: Map<String, String> = emptyMap(),
    @SerialName("earned_at") val earnedAt: String
)