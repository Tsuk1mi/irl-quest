package com.irlquest.shared.network

import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        createHttpClientConfig().invoke(this)
    }
}

