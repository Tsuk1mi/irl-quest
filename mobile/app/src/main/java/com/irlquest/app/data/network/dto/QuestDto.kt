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
    val description: String?,
    @SerialName("reward_experience") val rewardExperience: Int?,
    @SerialName("reward_description") val rewardDescription: String? = null,
    @SerialName("completion_percentage") val completionPercentage: Int = 0,
    val difficulty: Int = 1,
    val priority: String = "medium",  // String, как на сервере
    val status: String = "active",
    @SerialName("quest_type") val questType: String? = null,
    val deadline: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("owner_id") val ownerId: Int? = null,
    val tasks: List<TaskDto> = emptyList()
)

@Serializable
data class CreateQuestRequest(
    val title: String,
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
    @SerialName("quest_type") val questType: String? = null
)

@Serializable
data class UpdateQuestRequest(
    val title: String? = null,
    val description: String? = null,
    @SerialName("reward_experience") val rewardExperience: Int? = null,
    @SerialName("reward_description") val rewardDescription: String? = null,
    val difficulty: Int? = null,
    val priority: String? = null,  // String, как на сервере
    val status: String? = null,
    val deadline: String? = null,
    val tags: List<String>? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("quest_type") val questType: String? = null,
    @SerialName("completion_percentage") val completionPercentage: Int? = null
)
