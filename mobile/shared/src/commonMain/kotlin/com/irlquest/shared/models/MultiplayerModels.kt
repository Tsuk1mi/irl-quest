package com.irlquest.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuildDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerialName("leader_id") val leaderId: Int,
    val level: Int,
    val experience: Int,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("max_members") val maxMembers: Int,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CreateGuildRequest(
    val name: String,
    val description: String? = null,
    @SerialName("max_members") val maxMembers: Int? = null
)

@Serializable
data class GuildMemberDto(
    val id: Int,
    @SerialName("guild_id") val guildId: Int,
    @SerialName("user_id") val userId: Int,
    val role: String,
    @SerialName("joined_at") val joinedAt: String,
    val username: String? = null,
    val level: Int? = null
)

@Serializable
data class CoopMissionDto(
    val id: Int,
    @SerialName("quest_id") val questId: Int,
    @SerialName("party_size") val partySize: Int,
    @SerialName("max_party_size") val maxPartySize: Int,
    @SerialName("leader_id") val leaderId: Int,
    val status: String,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("created_at") val createdAt: String,
    val quest: com.irlquest.shared.models.QuestDto? = null,
    val members: List<PartyMemberDto> = emptyList()
)

@Serializable
data class CreateCoopMissionRequest(
    @SerialName("quest_id") val questId: Int,
    @SerialName("max_party_size") val maxPartySize: Int,
    @SerialName("is_public") val isPublic: Boolean = true
)

@Serializable
data class JoinMissionRequest(
    @SerialName("mission_id") val missionId: Int,
    @SerialName("preferred_role") val preferredRole: String
)

@Serializable
data class PartyMemberDto(
    val id: Int,
    @SerialName("mission_id") val missionId: Int,
    @SerialName("user_id") val userId: Int,
    val role: String,
    val contribution: Int,
    @SerialName("joined_at") val joinedAt: String,
    val username: String? = null,
    val level: Int? = null,
    @SerialName("character_class") val characterClass: String? = null
)

