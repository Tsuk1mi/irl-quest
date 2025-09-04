package com.irlquest.app.data.network

import okhttp3.Interceptor
import okhttp3.Response
import com.irlquest.app.TokenStorage

object TokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val token = TokenStorage.getToken()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}

