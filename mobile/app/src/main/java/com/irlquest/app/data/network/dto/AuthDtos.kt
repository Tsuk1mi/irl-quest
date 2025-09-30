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
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer"
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

@Serializable
data class UserOutResponse(
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

@Serializable
data class UserCreateRequest(
    val email: String,
    val username: String,
    val password: String
)
