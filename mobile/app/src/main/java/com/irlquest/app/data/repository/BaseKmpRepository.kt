package com.irlquest.app.data.repository

import com.irlquest.app.TokenStorage

abstract class BaseKmpRepository {
    protected fun currentToken(): String? = TokenStorage.getToken()

    protected fun requireToken(): String =
        currentToken() ?: throw IllegalStateException("Authentication token is missing. Please log in again.")

    protected fun authHeaders(token: String? = currentToken()): Map<String, String> =
        token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
}


