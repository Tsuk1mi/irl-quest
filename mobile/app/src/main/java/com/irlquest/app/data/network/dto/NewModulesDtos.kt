package com.irlquest.app.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Dice System DTOs
@Serializable
data class RollDiceRequest(
    @SerialName("dice_type") val diceType: String,
    val modifier: Int? = null,
    @SerialName("quest_id") val questId: Int? = null,
    @SerialName("action_description") val actionDescription: String? = null
)

@Serializable
data class RollDiceResponse(
    @SerialName("dice_type") val diceType: String,
    val result: Int,
    val modifier: Int,
    val total: Int,
    @SerialName("is_critical_success") val isCriticalSuccess: Boolean,
    @SerialName("is_critical_failure") val isCriticalFailure: Boolean,
    val timestamp: Long
)

@Serializable
data class MultiRollRequest(
    @SerialName("dice_type") val diceType: String,
    val count: Int,
    val modifier: Int? = null,
    @SerialName("keep_highest") val keepHighest: Int? = null,
    @SerialName("keep_lowest") val keepLowest: Int? = null
)

@Serializable
data class MultiRollResult(
    @SerialName("dice_type") val diceType: String,
    val rolls: List<Int>,
    @SerialName("kept_rolls") val keptRolls: List<Int>,
    val modifier: Int,
    val total: Int,
    val average: Float
)

@Serializable
data class SkillCheckRequest(
    val skill: String,
    val difficulty: Int,
    @SerialName("quest_id") val questId: Int? = null
)

@Serializable
data class SkillCheckResult(
    val skill: String,
    val roll: RollDiceResponse,
    val difficulty: Int,
    val success: Boolean,
    @SerialName("degree_of_success") val degreeOfSuccess: String,
    val description: String
)

@Serializable
data class DiceTypeInfo(
    @SerialName("dice_type") val diceType: String,
    val sides: Int,
    val emoji: String,
    val name: String,
    val description: String
)

@Serializable
data class SkillInfo(
    val skill: String,
    val name: String,
    val description: String,
    val stat: String
)

// Character System DTOs
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
    @SerialName("stat_name") val statName: String,
    @SerialName("current_value") val currentValue: Int,
    val modifier: Int,
    @SerialName("quest_type_bonuses") val questTypeBonuses: List<QuestTypeBonus>,
    @SerialName("success_chance_bonus") val successChanceBonus: Float,
    @SerialName("reward_bonus") val rewardBonus: Float,
    @SerialName("time_reduction") val timeReduction: Float
)

@Serializable
data class QuestTypeBonus(
    @SerialName("quest_type") val questType: String,
    @SerialName("bonus_description") val bonusDescription: String
)

@Serializable
data class SelectClassRaceRequest(
    @SerialName("class") val characterClass: String,
    val race: String
)

@Serializable
data class IncreaseStatRequest(
    @SerialName("stat_name") val statName: String,
    val amount: Int? = null
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

@Serializable
data class LevelUpResult(
    val success: Boolean,
    @SerialName("new_level") val newLevel: Int,
    @SerialName("stat_points_gained") val statPointsGained: Int,
    @SerialName("unlocked_features") val unlockedFeatures: List<String>
)

// ML Inference DTOs
@Serializable
data class TagsRequest(
    val text: String,
    @SerialName("max_tags") val maxTags: Int? = null
)

@Serializable
data class TagsResponse(
    val tags: List<TagPrediction>,
    @SerialName("processing_time_ms") val processingTimeMs: Long
)

@Serializable
data class TagPrediction(
    val tag: String,
    val confidence: Float,
    @SerialName("requires_review") val requiresReview: Boolean
)

@Serializable
data class DifficultyRequest(
    val title: String,
    val description: String? = null
)

@Serializable
data class DifficultyResponse(
    val difficulty: Int,
    val confidence: Float,
    val factors: List<DifficultyFactor>,
    @SerialName("requires_review") val requiresReview: Boolean,
    @SerialName("processing_time_ms") val processingTimeMs: Long
)

@Serializable
data class DifficultyFactor(
    val factor: String,
    val impact: Float,
    val explanation: String
)

@Serializable
data class TransformRequest(
    val title: String,
    val description: String? = null,
    val difficulty: Int? = null,
    @SerialName("user_level") val userLevel: Int? = null,
    @SerialName("preferred_style") val preferredStyle: String? = null
)

@Serializable
data class TransformResponse(
    @SerialName("fantasy_title") val fantasyTitle: String,
    @SerialName("fantasy_description") val fantasyDescription: String,
    @SerialName("suggested_rewards") val suggestedRewards: Rewards,
    @SerialName("suggested_difficulty") val suggestedDifficulty: Int,
    val confidence: Float,
    @SerialName("requires_review") val requiresReview: Boolean,
    @SerialName("style_used") val styleUsed: String,
    @SerialName("processing_time_ms") val processingTimeMs: Long
)

@Serializable
data class Rewards(
    val experience: Int,
    val gold: Int,
    val items: List<String>
)

@Serializable
data class RecommendationsRequest(
    @SerialName("user_id") val userId: Int,
    val limit: Int? = null,
    @SerialName("exclude_completed") val excludeCompleted: Boolean? = null
)

@Serializable
data class RecommendationsResponse(
    val quests: List<QuestRecommendation>,
    val reasoning: String,
    @SerialName("processing_time_ms") val processingTimeMs: Long
)

@Serializable
data class QuestRecommendation(
    val title: String,
    val description: String,
    val difficulty: Int,
    @SerialName("estimated_time_minutes") val estimatedTimeMinutes: Int,
    val tags: List<String>,
    val score: Float,
    val reasons: List<String>
)

// Quest Suggestions DTOs
@Serializable
data class QuestSuggestionResponse(
    val quest: QuestDto? = null,
    val message: String
)

@Serializable
data class MergeSuggestionsResponse(
    val suggestions: List<MergeSuggestion>
)

@Serializable
data class MergeSuggestion(
    @SerialName("quest_ids") val questIds: List<Int>,
    @SerialName("suggested_title") val suggestedTitle: String
)

@Serializable
data class AcceptQuestRequest(
    val title: String,
    val description: String? = null
)

// Geolocation DTOs
@Serializable
data class CreateGeoZoneRequest(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("radius_meters") val radiusMeters: Double,
    @SerialName("zone_type") val zoneType: String
)

@Serializable
data class GeoZone(
    val id: Int,
    val name: String,
    val center: Location,
    @SerialName("radius_meters") val radiusMeters: Double,
    @SerialName("zone_type") val zoneType: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class CheckLocationRequest(
    val latitude: Double,
    val longitude: Double,
    @SerialName("quest_id") val questId: Int? = null
)

@Serializable
data class LocationCheckResponse(
    @SerialName("in_zones") val inZones: List<GeoZoneInfo>,
    @SerialName("triggered_quests") val triggeredQuests: List<Int>,
    @SerialName("ar_markers") val arMarkers: List<ARMarker>
)

@Serializable
data class GeoZoneInfo(
    @SerialName("zone_id") val zoneId: Int,
    @SerialName("zone_name") val zoneName: String,
    @SerialName("zone_type") val zoneType: String,
    @SerialName("distance_meters") val distanceMeters: Double
)

@Serializable
data class ARMarker(
    val id: Int,
    val name: String,
    val description: String? = null,
    val location: Location,
    @SerialName("marker_type") val markerType: String,
    @SerialName("quest_id") val questId: Int? = null,
    @SerialName("is_collected") val isCollected: Boolean
)

@Serializable
data class UploadImageRequest(
    @SerialName("quest_id") val questId: Int,
    @SerialName("image_data") val imageData: String, // Base64
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class ImageVerificationResponse(
    @SerialName("verification_id") val verificationId: Int,
    val status: String,
    @SerialName("ai_confidence") val aiConfidence: Float,
    @SerialName("detected_objects") val detectedObjects: List<DetectedObject>,
    @SerialName("requires_review") val requiresReview: Boolean,
    @SerialName("auto_delete_at") val autoDeleteAt: String
)

@Serializable
data class DetectedObject(
    val label: String,
    val confidence: Float,
    @SerialName("bounding_box") val boundingBox: BoundingBox? = null
)

@Serializable
data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

@Serializable
data class ConsentRequest(
    @SerialName("camera_consent") val cameraConsent: Boolean,
    @SerialName("location_consent") val locationConsent: Boolean,
    @SerialName("data_processing_consent") val dataProcessingConsent: Boolean
)

// Config DTOs
@Serializable
data class ClientConfig(
    @SerialName("api_version") val apiVersion: String,
    @SerialName("server_url") val serverUrl: String,
    val features: ClientFeatures,
    val limits: ClientLimits
)

@Serializable
data class ClientFeatures(
    @SerialName("oauth_enabled") val oauthEnabled: Boolean,
    @SerialName("ar_enabled") val arEnabled: Boolean,
    @SerialName("multiplayer_enabled") val multiplayerEnabled: Boolean,
    @SerialName("image_processing_enabled") val imageProcessingEnabled: Boolean,
    @SerialName("mfa_enabled") val mfaEnabled: Boolean
)

@Serializable
data class ClientLimits(
    @SerialName("max_quest_title_length") val maxQuestTitleLength: Int,
    @SerialName("max_quest_description_length") val maxQuestDescriptionLength: Int
)

// ML Config
@Serializable
data class MlConfig(
    @SerialName("tags_confidence_threshold") val tagsConfidenceThreshold: Float,
    @SerialName("difficulty_confidence_threshold") val difficultyConfidenceThreshold: Float,
    @SerialName("transform_confidence_threshold") val transformConfidenceThreshold: Float,
    @SerialName("enable_human_in_loop") val enableHumanInLoop: Boolean
)

