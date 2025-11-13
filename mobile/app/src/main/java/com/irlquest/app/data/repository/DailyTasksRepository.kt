package com.irlquest.app.data.repository

import com.irlquest.app.data.network.dto.*
import com.irlquest.app.data.network.dto.CreateTaskRequest
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DailyTasksRepository : BaseKmpRepository() {
    private val statsRepo = StatsRepository()
    private val taskRepo = TaskRepository()
    private val questRepo = QuestRepository()

    /**
     * Анализирует активность пользователя и генерирует ежедневные задачи
     */
    suspend fun generateDailyTasksFromActivity(): List<TaskDto> {
        return try {
            // Получаем статистику за последние дни
            val dailyStats = statsRepo.getDailyStats()
            // Получаем текущие задачи и квесты для анализа паттернов
            val currentTasks = taskRepo.listTasks()
            val currentQuests = questRepo.listQuests()
            
            // Анализируем активность
            val activityPatterns = analyzeActivityPatterns(dailyStats, currentTasks, currentQuests)
            
            // Генерируем задачи на основе паттернов
            val generatedTasks = mutableListOf<TaskDto>()
            
            // 1. Задачи на основе частых тегов
            activityPatterns.frequentTags.forEach { tag ->
                val taskTitle = generateTaskTitleFromTag(tag, activityPatterns)
                if (taskTitle != null) {
                    val task = createDailyTask(
                        title = taskTitle,
                        description = "Ежедневная задача на основе вашей активности",
                        tags = listOf(tag, "daily", "auto-generated")
                    )
                    generatedTasks.add(task)
                }
            }
            
            // 2. Задачи на основе времени активности
            if (activityPatterns.mostActiveTime != null) {
                val task = createDailyTask(
                    title = "Планирование на ${activityPatterns.mostActiveTime}",
                    description = "В это время вы наиболее продуктивны",
                    tags = listOf("planning", "daily", "auto-generated")
                )
                generatedTasks.add(task)
            }
            
            // 3. Задачи на основе незавершенных квестов
            val incompleteQuests = currentQuests.filter { 
                it.completionPercentage ?: 0 < 100 
            }
            incompleteQuests.take(2).forEach { quest ->
                val task = createDailyTask(
                    title = "Продолжить квест: ${quest.title}",
                    description = "Прогресс: ${quest.completionPercentage ?: 0}%",
                    tags = listOf("quest", "daily", "auto-generated"),
                    questId = quest.id
                )
                generatedTasks.add(task)
            }
            
            // 4. Задачи на основе статистики продуктивности
            if (activityPatterns.averageProductivity < 50) {
                val task = createDailyTask(
                    title = "Повысить продуктивность",
                    description = "Ваша средняя продуктивность: ${activityPatterns.averageProductivity.toInt()}%",
                    tags = listOf("productivity", "daily", "auto-generated")
                )
                generatedTasks.add(task)
            }
            
            Timber.d("DailyTasksRepository: Generated ${generatedTasks.size} daily tasks")
            generatedTasks
        } catch (e: Exception) {
            Timber.e(e, "DailyTasksRepository: Failed to generate daily tasks")
            emptyList()
        }
    }
    
    private suspend fun createDailyTask(
        title: String,
        description: String,
        tags: List<String>,
        questId: Int? = null
    ): TaskDto {
        val request = CreateTaskRequest(
            title = title,
            description = description,
            priority = "medium",
            experienceReward = 15,
            estimatedDuration = null,
            difficulty = 1,
            questId = questId,
            deadline = null,
            tags = tags
        )
        return taskRepo.createTaskForQuest(request)
    }
    
    private fun analyzeActivityPatterns(
        dailyStats: List<DailyStatsDto>,
        tasks: List<TaskDto>,
        quests: List<QuestDto>
    ): ActivityPatterns {
        // Анализ тегов
        val allTags = (tasks.flatMap { it.tags } + quests.flatMap { it.tags ?: emptyList() })
        val tagFrequency = allTags.groupingBy { it }.eachCount()
        val frequentTags = tagFrequency.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        // Анализ времени активности (упрощенный - на основе дат создания)
        val mostActiveTime = if (dailyStats.isNotEmpty()) {
            // Берем день с наибольшей активностью
            dailyStats.maxByOrNull { it.tasksCompleted }?.let { "день" }
        } else null
        
        // Средняя продуктивность (на основе выполненных задач)
        val averageProductivity = if (dailyStats.isNotEmpty()) {
            val avgCompletion = dailyStats.map { 
                if (it.tasksTotal > 0) (it.tasksCompleted.toFloat() / it.tasksTotal) * 100f else 0f
            }.average().toFloat()
            avgCompletion
        } else 50f
        
        return ActivityPatterns(
            frequentTags = frequentTags,
            mostActiveTime = mostActiveTime,
            averageProductivity = averageProductivity
        )
    }
    
    private fun generateTaskTitleFromTag(tag: String, patterns: ActivityPatterns): String? {
        val tagToTaskMap = mapOf(
            "study" to "Изучить новый материал",
            "work" to "Выполнить рабочую задачу",
            "exercise" to "Сделать физические упражнения",
            "reading" to "Прочитать главу книги",
            "coding" to "Написать код",
            "learning" to "Изучить что-то новое",
            "health" to "Позаботиться о здоровье",
            "social" to "Встретиться с друзьями",
            "creative" to "Заняться творчеством",
            "planning" to "Составить план на день"
        )
        
        return tagToTaskMap[tag.lowercase()] ?: "Задача: $tag"
    }
    
    private data class ActivityPatterns(
        val frequentTags: List<String>,
        val mostActiveTime: String?,
        val averageProductivity: Float
    )
}

