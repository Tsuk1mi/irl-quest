package com.irlquest.shared.network

import io.ktor.client.*
import io.ktor.client.engine.android.*

actual fun createHttpClient(): HttpClient {
    return HttpClient(Android) {
        createHttpClientConfig().invoke(this)
    }
}

