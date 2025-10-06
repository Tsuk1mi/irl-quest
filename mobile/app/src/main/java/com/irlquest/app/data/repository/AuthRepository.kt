package com.irlquest.app.data.repository

import com.irlquest.app.TokenStorage
import com.irlquest.app.data.network.RetrofitClient
import com.irlquest.app.data.network.dto.LoginRequest
import com.irlquest.app.data.network.dto.RegisterRequest
import com.irlquest.app.data.network.dto.UserDto

class AuthRepository {
    private val api = RetrofitClient.apiService

    suspend fun login(username: String, password: String): UserDto {
        val resp = api.login(LoginRequest(username = username, password = password))
        if (!resp.isSuccessful) {
            val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
            throw Exception(err ?: "Login failed: ${resp.code()}")
        }
        val body = resp.body() ?: throw Exception("Login failed: empty response")
        // Сохранить токен
        TokenStorage.setToken(body.accessToken)
        return body.user
    }

    suspend fun register(email: String, username: String, password: String): UserDto {
        val resp = api.register(RegisterRequest(email = email, username = username, password = password))
        if (!resp.isSuccessful) {
            val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
            throw Exception(err ?: "Register failed: ${resp.code()}")
        }
        val body = resp.body() ?: throw Exception("Register failed: empty response")
        // Если сервер вернул токен, сохранить (если есть) - некоторые реализации могут вернуть UserDto only
        // Здесь ожидаем, что регистрация возвращает UserDto without token; логин отдельно сохранит токен
        return body
    }

    suspend fun getMe(): UserDto? {
        val resp = api.getMe()
        if (resp.isSuccessful) {
            return resp.body()
        } else {
            // Если неавторизован — очистим токен
            if (resp.code() == 401) {
                TokenStorage.clear()
                return null
            }
            val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
            throw Exception(err ?: "getMe failed: ${resp.code()}")
        }
    }

    fun logout() {
        TokenStorage.clear()
    }
}
