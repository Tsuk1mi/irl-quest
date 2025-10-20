package com.irlquest.app.data.ml

import com.irlquest.app.data.network.dto.TaskDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PredictedAttributes(
    val priority: String,
    val difficulty: Int,
    val tags: List<String>,
    val estimatedDuration: Int?
)

object MLPredictor {
    // Заглушка: простая эвристика/симуляция ML. Замените вызовом реальной модели/сервиса при необходимости.
    suspend fun predictAttributes(title: String, description: String?): PredictedAttributes = withContext(Dispatchers.Default) {
        // Простая эвристика: длина описания -> сложность; ключевые слова -> теги
        val text = (title + " " + (description ?: "")).lowercase()
        val tags = mutableListOf<String>()
        if (text.contains("email") || text.contains("письм")) tags.add("email")
        if (text.contains("meeting") || text.contains("встреч")) tags.add("meeting")
        if (text.contains("buy") || text.contains("куп")) tags.add("shopping")
        if (tags.isEmpty()) tags.add("general")

        val difficulty = when {
            text.length < 40 -> 1
            text.length < 120 -> 2
            text.length < 300 -> 3
            else -> 4
        }

        val priority = when {
            text.contains("urgent") || text.contains("срочно") -> "high"
            text.contains("tomorrow") || text.contains("завтра") -> "high"
            difficulty >= 3 -> "high"
            difficulty == 2 -> "medium"
            else -> "low"
        }

        val estimated = when (difficulty) {
            1 -> 15
            2 -> 30
            3 -> 60
            4 -> 120
            else -> 30
        }

        PredictedAttributes(priority = priority, difficulty = difficulty, tags = tags, estimatedDuration = estimated)
    }
}

