package com.irlquest.app.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Запросы аутентификации
@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val timezone: String? = null
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

// Ответы аутентификации
@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: Int,
    val email: String,
    val username: String,
    @SerialName("is_active") val isActive: Boolean,
    val level: Int,
    val experience: Int,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val timezone: String,
    @SerialName("last_login") val lastLogin: String? = null,
    val settings: Map<String, String> = emptyMap(),
    @SerialName("created_at") val createdAt: String
)

// Quest DTOs
@Serializable
data class QuestDto(
    val id: Int,
    val title: String,
    val description: String? = null,
    val difficulty: Int,
    val status: String,
    val priority: String,
    val deadline: String? = null,
    @SerialName("completion_percentage") val completionPercentage: Int,
    @SerialName("reward_experience") val rewardExperience: Int,
    @SerialName("reward_description") val rewardDescription: String? = null,
    val tags: List<String>,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("quest_type") val questType: String,
    val metadata: Map<String, String> = emptyMap(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("tasks_count") val tasksCount: Int? = null,
    @SerialName("completed_tasks_count") val completedTasksCount: Int? = null
)

@Serializable
data class CreateQuestRequest(
    val title: String,
    val description: String? = null,
    val difficulty: Int? = null,
    val priority: String? = null,
    val deadline: String? = null,
    @SerialName("reward_experience") val rewardExperience: Int? = null,
    @SerialName("reward_description") val rewardDescription: String? = null,
    val tags: List<String>? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("quest_type") val questType: String? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class UpdateQuestRequest(
    val title: String? = null,
    val description: String? = null,
    val difficulty: Int? = null,
    val status: String? = null,
    val priority: String? = null,
    val deadline: String? = null,
    @SerialName("reward_experience") val rewardExperience: Int? = null,
    @SerialName("reward_description") val rewardDescription: String? = null,
    val tags: List<String>? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("quest_type") val questType: String? = null,
    val metadata: Map<String, String>? = null
)

// Task DTOs  
@Serializable
data class TaskDto(
    val id: Int,
    val title: String,
    val description: String? = null,
    val completed: Boolean,
    val status: String,
    val priority: String,
    val deadline: String? = null,
    @SerialName("estimated_duration") val estimatedDuration: Int? = null,
    @SerialName("actual_duration") val actualDuration: Int? = null,
    val difficulty: Int,
    @SerialName("experience_reward") val experienceReward: Int,
    val tags: List<String>,
    @SerialName("location_name") val locationName: String? = null,
    val subtasks: Map<String, String> = emptyMap(),
    val notes: String? = null,
    val attachments: List<String>,
    @SerialName("completion_proof") val completionProof: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("quest_id") val questId: Int? = null
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    @SerialName("quest_id") val questId: Int? = null,
    val priority: String? = null,
    val deadline: String? = null,
    @SerialName("estimated_duration") val estimatedDuration: Int? = null,
    val difficulty: Int? = null,
    @SerialName("experience_reward") val experienceReward: Int? = null,
    val tags: List<String>? = null,
    @SerialName("location_name") val locationName: String? = null,
    val subtasks: Map<String, String>? = null,
    val notes: String? = null,
    val attachments: List<String>? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val completed: Boolean? = null,
    val status: String? = null,
    val priority: String? = null,
    val deadline: String? = null,
    @SerialName("estimated_duration") val estimatedDuration: Int? = null,
    @SerialName("actual_duration") val actualDuration: Int? = null,
    val difficulty: Int? = null,
    @SerialName("experience_reward") val experienceReward: Int? = null,
    val tags: List<String>? = null,
    @SerialName("location_name") val locationName: String? = null,
    val subtasks: Map<String, String>? = null,
    val notes: String? = null,
    val attachments: List<String>? = null,
    @SerialName("completion_proof") val completionProof: String? = null,
    val metadata: Map<String, String>? = null
)