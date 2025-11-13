package com.irlquest.shared.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ApiClient(
    baseUrl: String,
    httpClient: HttpClient = createHttpClient()
) {
    public val apiBaseUrl = "$baseUrl/api/v1"
    public val client = httpClient
    
    suspend inline fun <reified T> get(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): T {
        return client.get("$apiBaseUrl$path") {
            headers.forEach { (key, value) ->
                header(key, value)
            }
        }.body()
    }
    
    suspend inline fun <reified T> post(
        path: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap()
    ): T {
        return client.post("$apiBaseUrl$path") {
            headers.forEach { (key, value) ->
                header(key, value)
            }
            contentType(ContentType.Application.Json)
            if (body != null) {
                setBody(body)
            }
        }.body()
    }
    
    suspend inline fun <reified T, reified B> postWithBody(
        path: String,
        body: B,
        headers: Map<String, String> = emptyMap()
    ): T {
        return client.post("$apiBaseUrl$path") {
            headers.forEach { (key, value) ->
                header(key, value)
            }
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }
    
    suspend inline fun <reified T> put(
        path: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap()
    ): T {
        return client.put("$apiBaseUrl$path") {
            headers.forEach { (key, value) ->
                header(key, value)
            }
            contentType(ContentType.Application.Json)
            if (body != null) {
                setBody(body)
            }
        }.body()
    }
    
    suspend inline fun <reified T> delete(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): T {
        return client.delete("$apiBaseUrl$path") {
            headers.forEach { (key, value) ->
                header(key, value)
            }
        }.body()
    }
}

