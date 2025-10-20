package com.irlquest.app.data.ml

import com.irlquest.app.data.network.dto.QuestGenerationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MLGenerator {
    // Простая локальная генерация квеста из задачи — заглушка ML-модели.
    // Возвращает структуру сходную с QuestGenerationResponse.
    suspend fun generateQuestFromTask(title: String, description: String?, priority: String, estimatedDuration: Int?, difficulty: Int, tags: List<String>): QuestGenerationResponse = withContext(Dispatchers.Default) {
        val cleaned = (title + ". " + (description ?: "")).trim()
        val qTitle = "Quest: " + if (cleaned.length <= 40) cleaned else cleaned.take(40) + "..."
        val qDesc = "Сгенерировано из задачи: " + cleaned

        val tasks = mutableListOf<String>()
        // Простая логика: разбить описание на предложения или на части по запятой
        if (!description.isNullOrBlank()) {
            val parts = description.split(Regex("[.!,;\\n]")).map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isNotEmpty()) {
                parts.take(5).forEach { tasks.add(it) }
            }
        }
        if (tasks.isEmpty()) {
            // если нет описания — предложим шаги из заголовка
            tasks.add("Подготовить: $title")
            tasks.add("Выполнить: $title")
        }

        QuestGenerationResponse(
            title = qTitle,
            description = qDesc,
            tasks = tasks,
            experienceReward = (difficulty * 10),
            estimatedTime = estimatedDuration ?: 30,
            difficulty = difficulty,
            theme = tags.firstOrNull() ?: "general"
        )
    }
}

