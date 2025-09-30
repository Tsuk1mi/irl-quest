package com.irlquest.app.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RagStudyRequest(
    val content: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class RagStudyResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String? = null,
    @SerialName("document_id") val documentId: String? = null
)

@Serializable
data class RagGenerateRequest(
    val query: String,
    @SerialName("max_tokens") val maxTokens: Int = 1000,
    @SerialName("temperature") val temperature: Double = 0.7
)

@Serializable
data class RagGenerateResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String? = null,
    @SerialName("generated_text") val generatedText: String? = null,
    @SerialName("sources") val sources: List<String> = emptyList(),
    @SerialName("metadata") val metadata: Map<String, String> = emptyMap()
)
