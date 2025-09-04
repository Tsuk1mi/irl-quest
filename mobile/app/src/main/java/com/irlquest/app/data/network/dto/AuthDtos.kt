package com.irlquest.app.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserCreateRequest(
    val email: String,
    val username: String,
    val password: String
)

@Serializable
data class UserOutResponse(
    val id: Int,
    val email: String,
    val username: String,
    val is_active: Boolean,
    val created_at: String
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String = "bearer"
)

@Serializable
data class TaskDto(
    val id: Int,
    val title: String,
    val description: String? = null,
    val completed: Boolean = false,
    val created_at: String,
    val owner_id: Int? = null
)

@Serializable
data class TaskCreateRequest(
    val title: String,
    val description: String? = null
)

@Serializable
data class TaskUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val completed: Boolean? = null
)

@Serializable
data class QuestDto(
    val id: Int,
    val title: String,
    val description: String? = null,
    val difficulty: Int = 1,
    val created_at: String,
    val owner_id: Int? = null
)

@Serializable
data class QuestCreateRequest(
    val title: String,
    val description: String? = null,
    val difficulty: Int = 1
)

@Serializable
data class UserUpdateRequest(
    val username: String? = null,
    val password: String? = null
)
