package com.irlquest.app.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestGenerationRequest(
    val todo: String,
    val theme: String,
    val difficulty: Int,
    val context: String? = null
)

@Serializable
data class QuestGenerationResponse(
    val title: String,
    val description: String,
    val tasks: List<String>,
    @SerialName("experience_reward") val experienceReward: Int,
    @SerialName("estimated_time") val estimatedTime: Int,
    val difficulty: Int,
    val theme: String
)

@Serializable
data class QuestDto(
    val id: Int,
    val title: String,
    val description: String,
    @SerialName("experience_reward") val experienceReward: Int,
    @SerialName("estimated_time") val estimatedTime: Int,
    @SerialName("completion_percentage") val completionPercentage: Int,
    val difficulty: Int,
    val priority: Int,
    val status: String,
    val theme: String,
    @SerialName("quest_type") val questType: String,
    @SerialName("completed_at") val completedAt: String?,
    @SerialName("created_at") val createdAt: String,
    val tasks: List<TaskDto>
)

@Serializable
data class CreateQuestRequest(
    val title: String,
    val description: String,
    @SerialName("experience_reward") val experienceReward: Int,
    @SerialName("estimated_time") val estimatedTime: Int,
    val difficulty: Int,
    val priority: Int,
    val theme: String,
    val tasks: List<CreateTaskRequest>
)

@Serializable
data class UpdateQuestRequest(
    val title: String? = null,
    val description: String? = null,
    @SerialName("experience_reward") val experienceReward: Int? = null,
    @SerialName("estimated_time") val estimatedTime: Int? = null,
    val difficulty: Int? = null,
    val priority: Int? = null,
    val theme: String? = null,
    val status: String? = null
)
