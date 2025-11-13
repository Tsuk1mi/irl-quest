package com.irlquest.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestDto(
    val id: Int,
    val title: String?,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val difficulty: Int? = null,
    @SerialName("completion_percentage") val completionPercentage: Int? = null,
    @SerialName("reward_experience") val rewardExperience: Int? = null,
    @SerialName("reward_description") val rewardDescription: String? = null,
    @SerialName("quest_type") val questType: String? = null,
    val deadline: String? = null,
    val tags: List<String>? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("owner_id") val ownerId: Int? = null,
    val tasks: List<TaskDto> = emptyList()
)

@Serializable
data class CreateQuestRequest(
    val title: String,
    val description: String? = null,
    val difficulty: Int = 1,
    val status: String = "active",
    val priority: String = "medium",
    @SerialName("reward_experience") val rewardExperience: Int? = null,
    @SerialName("reward_description") val rewardDescription: String? = null,
    @SerialName("quest_type") val questType: String = "manual",
    val tags: List<String>? = null,
    @SerialName("is_public") val isPublic: Boolean = false
)

@Serializable
data class UpdateQuestRequest(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val status: String? = null
)

