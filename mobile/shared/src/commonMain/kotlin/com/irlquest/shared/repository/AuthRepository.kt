package com.irlquest.shared.repository

import com.irlquest.shared.models.*
import com.irlquest.shared.network.ApiClient

class AuthRepository(
    private val apiClient: ApiClient
) {
    suspend fun login(username: String, password: String): LoginResponse {
        return apiClient.postWithBody(
            "/auth/login",
            LoginRequest(username, password)
        )
    }
    
    suspend fun register(username: String, email: String, password: String): UserDto {
        return apiClient.postWithBody(
            "/auth/register",
            RegisterRequest(username, email, password)
        )
    }
    
    suspend fun getMe(token: String): UserDto {
        return apiClient.get(
            "/auth/me",
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
}

