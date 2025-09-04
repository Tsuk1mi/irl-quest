package com.irlquest.app.data.repository

import com.irlquest.app.TokenStorage
import com.irlquest.app.data.network.RetrofitClient
import com.irlquest.app.data.network.dto.TokenResponse
import com.irlquest.app.data.network.dto.UserOutResponse

class AuthRepository {
    private val api = RetrofitClient.apiService

    suspend fun login(username: String, password: String): TokenResponse {
        val resp = api.token(username, password)
        TokenStorage.saveToken(resp.access_token)
        return resp
    }

    suspend fun register(email: String, username: String, password: String) {
        api.register(com.irlquest.app.data.network.dto.UserCreateRequest(email, username, password))
    }

    suspend fun me(): UserOutResponse {
        return api.me()
    }
}
