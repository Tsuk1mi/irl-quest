package com.irlquest.app.data.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException

class RetryInterceptor(private val maxRetries: Int = 2, private val initialDelayMs: Long = 500) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastException: IOException? = null
        while (true) {
            try {
                return chain.proceed(chain.request())
            } catch (e: IOException) {
                // Если вызов был отменён или произошёл таймаут — не ретраить
                if (e is InterruptedIOException || e.message?.contains("Canceled", ignoreCase = true) == true) {
                    throw e
                }
                lastException = e
                if (attempt >= maxRetries) {
                    // пробуем выбросить последнюю ошибку
                    throw lastException
                }
                try {
                    Thread.sleep(initialDelayMs * (1L shl attempt))
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw e
                }
                attempt++
            }
        }
    }
}
