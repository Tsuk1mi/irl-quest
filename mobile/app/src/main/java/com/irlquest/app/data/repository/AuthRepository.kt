package com.irlquest.app.data.repository

import com.irlquest.app.TokenStorage
import com.irlquest.app.data.network.RetrofitClient
import com.irlquest.app.data.network.dto.LoginRequest
import com.irlquest.app.data.network.dto.RegisterRequest
import com.irlquest.app.data.network.dto.LoginResponse
import com.irlquest.app.data.network.dto.UserDto

class AuthRepository {
    private val api = RetrofitClient.apiService

    suspend fun login(username: String, password: String): LoginResponse {
        val resp = api.login(LoginRequest(username, password))
        TokenStorage.saveToken(resp.body()!!.accessToken)
        return resp.body()!!
    }

    suspend fun register(email: String, username: String, password: String) {
        api.register(RegisterRequest(email, username, password))
    }

    suspend fun me(): UserDto {
        return api.getMe().body()!!
    }
}
