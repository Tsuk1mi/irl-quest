package com.irlquest.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CharacterProfile(
    val character: CharacterDto,
    @SerialName("stat_influences") val statInfluences: List<StatInfluence>,
    @SerialName("can_level_up") val canLevelUp: Boolean,
    @SerialName("experience_to_next_level") val experienceToNextLevel: Int,
    @SerialName("available_stat_points") val availableStatPoints: Int
)

@Serializable
data class CharacterDto(
    @SerialName("user_id") val userId: Int,
    @SerialName("class") val characterClass: String,
    val race: String,
    val stats: CharacterStats,
    val level: Int,
    val experience: Int,
    val gold: Int
)

@Serializable
data class CharacterStats(
    val strength: Int,
    val intelligence: Int,
    val dexterity: Int,
    val charisma: Int,
    val luck: Int
)

@Serializable
data class StatInfluence(
    val stat: String,
    val influence: String,
    val value: Int
)

@Serializable
data class SelectClassRaceRequest(
    @SerialName("character_class") val characterClass: String,
    val race: String
)

@Serializable
data class IncreaseStatRequest(
    val stat: String
)

@Serializable
data class LevelUpResult(
    val success: Boolean,
    @SerialName("new_level") val newLevel: Int,
    @SerialName("stat_points_gained") val statPointsGained: Int,
    @SerialName("unlocked_features") val unlockedFeatures: List<String>
)

@Serializable
data class ClassInfo(
    @SerialName("class") val characterClass: String,
    @SerialName("name_ru") val nameRu: String,
    val description: String,
    @SerialName("stat_bonuses") val statBonuses: StatBonuses,
    @SerialName("recommended_for") val recommendedFor: List<String>
)

@Serializable
data class RaceInfo(
    val race: String,
    @SerialName("name_ru") val nameRu: String,
    val description: String,
    @SerialName("stat_bonuses") val statBonuses: StatBonuses
)

@Serializable
data class StatBonuses(
    val strength: Int,
    val intelligence: Int,
    val dexterity: Int,
    val charisma: Int,
    val luck: Int
)

