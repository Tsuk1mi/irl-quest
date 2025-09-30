package com.irlquest.app.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyStatsDto(
    val date: String,
    @SerialName("tasks_completed") val tasksCompleted: Int,
    @SerialName("tasks_total") val tasksTotal: Int,
    @SerialName("quests_completed") val questsCompleted: Int,
    @SerialName("quests_total") val questsTotal: Int,
    @SerialName("experience_gained") val experienceGained: Int,
    @SerialName("focus_time") val focusTime: Int,
    @SerialName("study_time") val studyTime: Int
)

@Serializable
data class TotalStatsDto(
    @SerialName("total_tasks_completed") val totalTasksCompleted: Int,
    @SerialName("total_quests_completed") val totalQuestsCompleted: Int,
    @SerialName("total_experience") val totalExperience: Int,
    @SerialName("current_level") val currentLevel: Int,
    @SerialName("next_level_experience") val nextLevelExperience: Int,
    @SerialName("total_focus_time") val totalFocusTime: Int,
    @SerialName("total_study_time") val totalStudyTime: Int,
    @SerialName("achievement_count") val achievementCount: Int
)
