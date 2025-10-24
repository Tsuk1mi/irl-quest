package com.irlquest.app.data.repository

import com.irlquest.app.TokenStorage
import com.irlquest.app.data.network.RetrofitClient
import com.irlquest.app.BuildConfig
import com.irlquest.app.data.network.dto.LoginRequest
import com.irlquest.app.data.network.dto.RegisterRequest
import com.irlquest.app.data.network.dto.UserDto
import com.irlquest.app.data.network.dto.LoginResponse
import java.time.Instant
import timber.log.Timber
import java.net.SocketTimeoutException
import java.io.IOException
import java.io.InterruptedIOException

class AuthRepository {
    private val api = RetrofitClient.apiService

    suspend fun login(username: String, password: String): UserDto {
        val resp = try {
            api.login(LoginRequest(username = username, password = password))
        } catch (e: InterruptedIOException) {
            Timber.e(e, "AuthRepository.login: network timeout/interrupt when connecting to API")
            throw Exception("Network timeout: failed to connect to server at ${BuildConfig.API_BASE_URL}.\n" +
                    "Verify the backend is running, bound to 0.0.0.0 (or the host IP), and that your device can reach ${BuildConfig.API_BASE_URL}. For emulator use 10.0.2.2.")
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "AuthRepository.login: socket timeout when connecting to API")
            throw Exception("Network socket timeout: failed to connect to server at ${BuildConfig.API_BASE_URL}.\n" +
                    "Make sure backend is running and reachable from the device.")
        } catch (e: IOException) {
            Timber.e(e, "AuthRepository.login: IO/network error when connecting to API")
            throw Exception("Network error: ${e.message}. Ensure your device is on the same network as the backend and that the backend is bound to a reachable address (0.0.0.0) and port is open.")
        }

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
        val meResp = try {
            api.getMe()
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "AuthRepository.getMe: socket timeout when calling /auth/me")
            // Не считаем это фатальной ошибкой — используем fallback
            null
        } catch (e: IOException) {
            Timber.e(e, "AuthRepository.getMe: IO/network error when calling /auth/me")
            null
        }

        if (meResp != null) {
            if (meResp.isSuccessful) {
                val me = meResp.body()
                if (me != null) return me
                else {
                    Timber.w("AuthRepository: /auth/me returned empty body despite success")
                    // fallthrough to fallback below
                }
            } else {
                // Если /auth/me вернул 404 — возможно API не реализует этот маршрут; не считаем это фатальной ошибкой
                if (meResp.code() == 404) {
                    Timber.w("AuthRepository: /auth/me returned 404 Not Found, will use fallback from login response if available")
                } else {
                    val err = try { meResp.errorBody()?.string() } catch (_: Exception) { null }
                    throw Exception(err ?: "Login succeeded but failed to fetch profile: ${meResp.code()}")
                }
            }
        }

        // Если /auth/me отсутствует или вернул пусто, попытаемся собрать минимальный объект UserDto из полей login response
        val fallbackId = when {
            (body is LoginResponse) -> body.userId
            else -> null
        }
        val fallbackUsername = when {
            (body is LoginResponse) -> body.username
            else -> null
        }

        val safeId = fallbackId ?: -1
        val safeUsername = fallbackUsername ?: username
        val now = Instant.now().toString()

        Timber.i("AuthRepository: creating fallback UserDto id=%d username=%s", safeId, safeUsername)
        // Собираем UserDto с безопасными значениями — эти поля могут быть не полными, но позволяют приложению продолжить работу
        val fallbackUser = UserDto(
            id = safeId,
            email = "",
            username = safeUsername,
            isActive = true,
            level = 0,
            experience = 0,
            gold = 0,
            avatarUrl = null,
            bio = null,
            timezone = "UTC",
            lastLogin = null,
            settings = emptyMap(),
            createdAt = now
        )

        return fallbackUser
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
        val resp = try {
            api.getMe()
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "AuthRepository.getMe: socket timeout when connecting to API")
            throw Exception("Network timeout while fetching profile. Please check the backend and network connectivity.")
        } catch (e: IOException) {
            Timber.e(e, "AuthRepository.getMe: IO/network error when connecting to API")
            throw Exception("Network error while fetching profile: ${e.message}")
        }

        if (resp.isSuccessful) {
            return resp.body()
        } else {
            // Если неавторизован — очистим токен
            if (resp.code() == 401) {
                TokenStorage.clear()
                return null
            }
            // Если маршрут отсутствует — вернём null вместо исключения
            if (resp.code() == 404) {
                Timber.w("AuthRepository.getMe: /auth/me returned 404 Not Found")
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
