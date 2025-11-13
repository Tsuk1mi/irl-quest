package com.irlquest.app.data.repository

import com.irlquest.app.BuildConfig
import com.irlquest.app.TokenStorage
import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.app.data.network.dto.LoginRequest
import com.irlquest.app.data.network.dto.LoginResponse
import com.irlquest.app.data.network.dto.RegisterRequest
import com.irlquest.app.data.network.dto.ProfileUpdateRequest
import com.irlquest.app.data.network.dto.UserDto
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.Instant
import timber.log.Timber

class AuthRepository : BaseKmpRepository() {
    private val apiClient = SharedRepositoryProvider.apiClient

    suspend fun login(username: String, password: String): UserDto {
        val response = try {
            apiClient.postWithBody<LoginResponse, LoginRequest>(
                path = "/auth/login",
                body = LoginRequest(username = username, password = password)
            )
        } catch (e: HttpRequestTimeoutException) {
            Timber.e(e, "AuthRepository.login: HTTP request timeout")
            throw Exception(
                "Network timeout: failed to reach ${BuildConfig.API_BASE_URL}. " +
                    "Verify that the backend is running and reachable (10.0.2.2 for emulator)."
            )
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "AuthRepository.login: socket timeout")
            throw Exception(
                "Connection timed out when contacting ${BuildConfig.API_BASE_URL}. " +
                    "Check backend availability and network connectivity."
            )
        } catch (e: ConnectException) {
            Timber.e(e, "AuthRepository.login: connection refused")
            throw Exception(
                "Unable to connect to ${BuildConfig.API_BASE_URL}. " +
                    "Ensure the backend is running and accessible from the device."
            )
        } catch (e: ClientRequestException) {
            Timber.e(e, "AuthRepository.login: client error ${e.response.status}")
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Login failed with status ${e.response.status.value}")
        } catch (e: ServerResponseException) {
            Timber.e(e, "AuthRepository.login: server error ${e.response.status}")
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Server error ${e.response.status.value} during login")
        } catch (e: IOException) {
            Timber.e(e, "AuthRepository.login: IO error")
            throw Exception("Network error: ${e.message}. Check your connection to ${BuildConfig.API_BASE_URL}.")
        } catch (e: Exception) {
            Timber.e(e, "AuthRepository.login: unexpected error")
            throw e
        }

        val token = response.accessTokenValue
        Timber.d(
            "AuthRepository: Login response token present=%s accessToken=%s refreshToken=%s",
            token != null,
            response.accessToken != null,
            response.refreshTokenValue != null
        )
        if (token.isNullOrEmpty()) {
            throw Exception("Login failed: server did not provide an access token")
        }
            TokenStorage.setToken(token)
        
        response.refreshTokenValue?.let {
            Timber.d("AuthRepository: received refresh token")
        }

        response.user?.let { return it }

        val me = runCatching {
            apiClient.get<UserDto>(
                path = "/auth/me",
                headers = authHeaders(token)
            )
        }.onFailure { error ->
            when (error) {
                is ClientRequestException -> {
                    if (error.response.status == HttpStatusCode.NotFound) {
                        Timber.w("AuthRepository: /auth/me is not available (404). Using fallback user.")
                    } else if (error.response.status == HttpStatusCode.Unauthorized) {
                        Timber.w("AuthRepository: /auth/me returned 401 despite fresh login.")
                        TokenStorage.clear()
            } else {
                        val message = runCatching { error.response.bodyAsText() }.getOrNull()
                        throw Exception(message ?: "Failed to fetch profile: ${error.response.status.value}")
                    }
                }

                is ServerResponseException -> {
                    val message = runCatching { error.response.bodyAsText() }.getOrNull()
                    throw Exception(message ?: "Server error ${error.response.status.value} while fetching profile")
                }

                is HttpRequestTimeoutException, is SocketTimeoutException -> {
                    Timber.w(error, "AuthRepository: timeout on /auth/me, using fallback user")
                }

                is IOException -> {
                    Timber.w(error, "AuthRepository: IO error on /auth/me, using fallback user")
                }
            }
        }.getOrNull()
        if (me != null) return me

        val fallbackUser = UserDto(
            id = response.userId ?: -1,
            email = response.user?.email ?: "",
            username = response.username ?: username,
            isActive = true,
            level = 0,
            experience = 0,
            gold = 0,
            avatarUrl = response.user?.avatarUrl,
            bio = response.user?.bio,
            timezone = response.user?.timezone ?: "UTC",
            lastLogin = response.user?.lastLogin,
            settings = response.user?.settings ?: emptyMap(),
            strength = response.user?.strength ?: 10,
            intelligence = response.user?.intelligence ?: 10,
            charisma = response.user?.charisma ?: 10,
            dexterity = response.user?.dexterity ?: 10,
            constitution = response.user?.constitution ?: 10,
            wisdom = response.user?.wisdom ?: 10,
            characterClass = response.user?.characterClass ?: "warrior",
            characterRace = response.user?.characterRace ?: "human",
            createdAt = response.user?.createdAt ?: Instant.now().toString()
        )
        Timber.i("AuthRepository: returning fallback user id=%d username=%s", fallbackUser.id, fallbackUser.username)
        return fallbackUser
    }

    suspend fun register(email: String, username: String, password: String): UserDto {
        return try {
            apiClient.postWithBody(
                path = "/auth/register",
                body = RegisterRequest(
                    email = email,
                    username = username,
                    password = password
                )
            )
        } catch (e: ClientRequestException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Register failed with status ${e.response.status.value}")
        } catch (e: ServerResponseException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Server error ${e.response.status.value} during registration")
        }
    }

    suspend fun getMe(): UserDto? {
        val token = currentToken() ?: return null
        return try {
            apiClient.get(
                path = "/auth/me",
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                Timber.w("AuthRepository.getMe: unauthorized, clearing token")
                TokenStorage.clear()
                null
            } else if (e.response.status == HttpStatusCode.NotFound) {
                Timber.w("AuthRepository.getMe: endpoint not found (404)")
                null
        } else {
                val message = runCatching { e.response.bodyAsText() }.getOrNull()
                throw Exception(message ?: "Failed to fetch profile: ${e.response.status.value}")
            }
        } catch (e: ServerResponseException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Server error ${e.response.status.value} while fetching profile")
        }
    }

    suspend fun updateProfile(
        username: String?,
        avatarUrl: String?,
        bio: String?
    ): UserDto {
        val token = requireToken()
        return try {
            apiClient.put(
                path = "/auth/profile",
                body = ProfileUpdateRequest(
                    username = username,
                    avatarUrl = avatarUrl,
                    bio = bio
                ),
                headers = authHeaders(token)
            )
        } catch (e: ClientRequestException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Не удалось обновить профиль: ${e.response.status.value}")
        } catch (e: ServerResponseException) {
            val message = runCatching { e.response.bodyAsText() }.getOrNull()
            throw Exception(message ?: "Ошибка сервера ${e.response.status.value} при обновлении профиля")
        }
    }

    fun logout() {
        TokenStorage.clear()
    }
}

