package com.irlquest.app.data.repository

import com.irlquest.app.TokenStorage
import com.irlquest.app.data.network.RetrofitClient
import com.irlquest.app.data.network.dto.LoginRequest
import com.irlquest.app.data.network.dto.RegisterRequest
import com.irlquest.app.data.network.dto.UserDto
import timber.log.Timber

class AuthRepository {
    private val api = RetrofitClient.apiService

    suspend fun login(username: String, password: String): UserDto {
        val resp = api.login(LoginRequest(username = username, password = password))
        if (!resp.isSuccessful) {
            val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
            throw Exception(err ?: "Login failed: ${resp.code()}")
        }
        val body = resp.body() ?: throw Exception("Login failed: empty response")

        // Попробуем извлечь токен из разных полей (accessToken или token)
        val token = body.accessToken ?: body.token
        if (token.isNullOrEmpty()) {
            Timber.w("AuthRepository: login response contains no token")
        } else {
            TokenStorage.setToken(token)
            Timber.d("AuthRepository: saved token, len=%d", token.length)
        }

        // Попробуем вернуть user из тела ответа, если он есть.
        if (body.user != null) {
            return body.user
        }

        // Если пользователь не вернулся в теле логина, запрашиваем профиль через /auth/me
        val meResp = api.getMe()
        if (meResp.isSuccessful) {
            val me = meResp.body()
            if (me != null) return me
            else throw Exception("Login succeeded but /auth/me returned empty body")
        } else {
            val err = try { meResp.errorBody()?.string() } catch (_: Exception) { null }
            throw Exception(err ?: "Login succeeded but failed to fetch profile: ${meResp.code()}")
        }
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
