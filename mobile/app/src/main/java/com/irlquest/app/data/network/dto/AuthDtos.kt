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
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token") val token: String? = null,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: UserDto? = null,
    @SerialName("user_id") val userId: Int? = null,
    val username: String? = null,
    @SerialName("client_ip") val clientIp: String? = null
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer"
)

@Serializable
data class UserDto(
    val id: Int = 0,
    val email: String = "",
    val username: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    val level: Int = 1,
    val experience: Int = 0,
    val gold: Int = 100,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val timezone: String = "UTC",
    @SerialName("last_login") val lastLogin: String? = null,
    val settings: Map<String, String> = emptyMap(),
    // D&D характеристики
    val strength: Int = 10,
    val intelligence: Int = 10,
    val charisma: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val wisdom: Int = 10,
    // Класс и раса персонажа
    @SerialName("character_class") val characterClass: String = "warrior",
    @SerialName("character_race") val characterRace: String = "human",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class UserOutResponse(
    val id: Int,
    val email: String,
    val username: String,
    @SerialName("is_active") val isActive: Boolean,
    val level: Int,
    val experience: Int,
    val gold: Int,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val timezone: String,
    @SerialName("last_login") val lastLogin: String? = null,
    val settings: Map<String, String> = emptyMap(),
    // D&D характеристики
    val strength: Int = 10,
    val intelligence: Int = 10,
    val charisma: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val wisdom: Int = 10,
    // Класс и раса персонажа
    @SerialName("character_class") val characterClass: String = "warrior",
    @SerialName("character_race") val characterRace: String = "human",
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class UserCreateRequest(
    val email: String,
    val username: String,
    val password: String
)
