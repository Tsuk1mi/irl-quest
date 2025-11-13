package com.irlquest.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: Int,
    val title: String,
    val description: String? = null,
    val completed: Boolean,
    val status: String,
    val priority: String,
    @SerialName("experience_reward") val experienceReward: Int,
    @SerialName("estimated_duration") val estimatedDuration: Int? = null,
    @SerialName("actual_duration") val actualDuration: Int? = null,
    val difficulty: Int,
    @SerialName("quest_id") val questId: Int? = null,
    val deadline: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("owner_id") val ownerId: Int? = null
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null,
    @SerialName("experience_reward") val experienceReward: Int? = null,
    @SerialName("estimated_duration") val estimatedDuration: Int? = null,
    val difficulty: Int? = null,
    @SerialName("quest_id") val questId: Int? = null,
    val deadline: String? = null,
    val tags: List<String>? = null,
    @SerialName("location_name") val locationName: String? = null
)

