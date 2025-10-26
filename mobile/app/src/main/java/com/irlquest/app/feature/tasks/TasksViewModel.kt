package com.irlquest.app.feature.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.irlquest.app.ui.viewmodel.AuthViewModel
import com.irlquest.app.data.repository.TaskRepository
import com.irlquest.app.data.network.dto.TaskDto
import timber.log.Timber

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED
}

enum class TaskPriority(val displayName: String) {
    LOW("Низкий"),
    MEDIUM("Средний"),
    HIGH("Высокий"),
    CRITICAL("Критический")
}

enum class TaskFilter(val displayName: String, val icon: ImageVector) {
    ALL("Все", Icons.Default.List),
    ACTIVE("Активные", Icons.Default.PlayArrow),
    COMPLETED("Завершенные", Icons.Default.CheckCircle),
    HIGH_PRIORITY("Важные", Icons.Default.PriorityHigh),
    OVERDUE("Просроченные", Icons.Default.Warning)
}

data class TaskUi(
    val id: Int,
    val title: String,
    val description: String,
    val completed: Boolean,
    val status: TaskStatus,
    val priority: TaskPriority,
    val deadline: String?,
    val isOverdue: Boolean,
    val estimatedDuration: Int?, // minutes
    val actualDuration: Int?, // minutes
    val difficulty: Int, // 1-5
    val experienceReward: Int,
    val tags: List<String>,
    val questId: Int?,
    val createdAt: String,
    val completedAt: String?,
    // ML-генерированное приключение
    val fantasyTitle: String? = null,
    val fantasyDescription: String? = null,
    val showFantasyVersion: Boolean = true // По умолчанию показываем фэнтези
)

data class TaskSummary(
    val total: Int,
    val completed: Int,
    val experienceGained: Int
)

data class TasksUiState(
    val tasks: List<TaskUi> = emptyList(),
    val isLoading: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showRewardDialog: Boolean = false,
    val lastCompletedTask: TaskUi? = null,
    val leveledUp: Boolean = false,
    val newLevel: Int? = null,
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val todaySummary: TaskSummary = TaskSummary(0, 0, 0),
    val error: String? = null
)


class TasksViewModel(
    private val authViewModel: AuthViewModel? = null,
    private val repo: TaskRepository = TaskRepository()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()
    
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val api = com.irlquest.app.data.network.RetrofitClient.apiService
    
    init {
        loadTasks()
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Загрузка задач с сервера
                val tasksDto = repo.listTasks()
                val tasks = tasksDto.map { dtoToUi(it) }
                
                Timber.d("TasksViewModel: Loaded ${tasks.size} tasks from server")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    tasks = tasks,
                    todaySummary = calculateTodaySummary(tasks)
                )
            } catch (e: Exception) {
                Timber.e(e, "TasksViewModel: Failed to load tasks")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    tasks = emptyList(),
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Конвертация TaskDto в TaskUi
     */
    private fun dtoToUi(dto: TaskDto): TaskUi {
        val priority = when (dto.priority?.lowercase()) {
            "critical" -> TaskPriority.CRITICAL
            "high" -> TaskPriority.HIGH
            "medium" -> TaskPriority.MEDIUM
            "low" -> TaskPriority.LOW
            else -> TaskPriority.MEDIUM
        }
        
        val status = when (dto.status?.lowercase()) {
            "completed" -> TaskStatus.COMPLETED
            "in_progress" -> TaskStatus.IN_PROGRESS
            "cancelled" -> TaskStatus.CANCELLED
            else -> TaskStatus.PENDING
        }
        
        // ML-генерация фэнтези версии
        val (fantasyTitle, fantasyDesc) = generateFantasyQuest(
            dto.title, 
            dto.description ?: "", 
            dto.difficulty ?: 1
        )
        
        return TaskUi(
            id = dto.id,
            title = dto.title,
            description = dto.description ?: "",
            completed = dto.completed ?: false,
            status = status,
            priority = priority,
            deadline = dto.deadline,
            isOverdue = false, // TODO: вычислить
            estimatedDuration = dto.estimatedDuration,
            actualDuration = null,
            difficulty = dto.difficulty ?: 1,
            experienceReward = dto.experienceReward ?: 10,
            tags = dto.tags ?: emptyList(),
            questId = dto.questId,
            createdAt = dto.createdAt ?: "",
            completedAt = dto.completedAt,
            fantasyTitle = fantasyTitle,
            fantasyDescription = fantasyDesc,
            showFantasyVersion = true
        )
    }
    
    fun createTask(title: String, description: String, priority: TaskPriority, difficulty: Int = 3, deadline: String? = null, aiPick: Boolean = false) {
        viewModelScope.launch {
            try {
                // Если пользователь попросил AI - используем умную систему расчёта
                val finalDifficulty = if (aiPick) {
                    calculateAIDifficulty(title, description)
                } else {
                    difficulty
                }

                // Расчёт наград на основе сложности и приоритета
                val xpReward = finalDifficulty * 10 + when (priority) {
                    TaskPriority.CRITICAL -> 20
                    TaskPriority.HIGH -> 10
                    TaskPriority.MEDIUM -> 5
                    TaskPriority.LOW -> 0
                }
                
                val priorityStr = when (priority) {
                    TaskPriority.CRITICAL -> "critical"
                    TaskPriority.HIGH -> "high"
                    TaskPriority.MEDIUM -> "medium"
                    TaskPriority.LOW -> "low"
                }
                
                val tags = extractTags(title, description)

                // 🌐 Создание задачи на сервере через API
                val request = com.irlquest.app.data.network.dto.CreateTaskRequest(
                    title = title,
                    description = description,
                    priority = priorityStr,
                    experienceReward = xpReward,
                    estimatedDuration = null,
                    difficulty = finalDifficulty,
                    questId = null,
                    deadline = deadline,
                    tags = tags
                )
                
                val createdDto = api.createTask(request).body()
                
                if (createdDto != null) {
                    // Конвертируем и добавляем в список
                    val newTask = dtoToUi(createdDto)
                    
                    Timber.d("TasksViewModel: Task created successfully, id=${newTask.id}")
                    
                    // Обновляем список
                    loadTasks()
                    
                    _uiState.value = _uiState.value.copy(
                        showCreateDialog = false
                    )
                } else {
                    throw Exception("Сервер вернул пустой ответ")
                }
            } catch (e: Exception) {
                Timber.e(e, "TasksViewModel: Failed to create task")
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка создания задачи: ${e.message}",
                    showCreateDialog = false
                )
            }
        }
    }
    
    /**
     * 🤖 ИИ-определение сложности задачи
     */
    private fun calculateAIDifficulty(title: String, description: String): Int {
        val text = "$title $description".lowercase()
        var difficulty = 2
        
        // Ключевые слова сложности (русский + английский)
        val complexKeywords = listOf(
            "сложн", "трудн", "тяжёл", "тяжел", "продвинут", "эксперт",
            "complex", "difficult", "challenging", "hard", "advanced"
        )
        val simpleKeywords = listOf(
            "прост", "лёгк", "легк", "быстр", "базов",
            "simple", "easy", "quick", "basic"
        )
        
        // Анализ
        if (complexKeywords.any { text.contains(it) }) difficulty += 2
        if (simpleKeywords.any { text.contains(it) }) difficulty -= 1
        if (text.split(" ").size > 15) difficulty += 1
        if (text.contains("презентация") || text.contains("экзамен") || text.contains("защита")) difficulty += 2
        
        return difficulty.coerceIn(1, 5)
    }
    
    /**
     * 🏷️ Извлечение тегов из текста
     */
    private fun extractTags(title: String, description: String): List<String> {
        val text = "$title $description".lowercase()
        val tags = mutableListOf<String>()
        
        if (text.contains("работ") || text.contains("проект") || text.contains("work") || text.contains("job")) {
            tags.add("работа")
        }
        if (text.contains("учи") || text.contains("изучи") || text.contains("курс") || text.contains("study")) {
            tags.add("обучение")
        }
        if (text.contains("спорт") || text.contains("трениро") || text.contains("здоров") || text.contains("exercise")) {
            tags.add("здоровье")
        }
        if (text.contains("магазин") || text.contains("купи") || text.contains("shop") || text.contains("buy")) {
            tags.add("покупки")
        }
        if (text.contains("убор") || text.contains("чист") || text.contains("дом") || text.contains("clean")) {
            tags.add("дом")
        }
        
        return tags
    }
    
    /**
     * 🎭 ML-генерация фэнтези квеста из обычной задачи
     */
    private fun generateFantasyQuest(title: String, description: String, difficulty: Int): Pair<String, String> {
        // Шаблоны названий квестов
        val titleTemplates = listOf(
            "⚔️ Священная миссия: {}",
            "🏆 Поиски артефакта: {}",
            "📜 Хроники: {}",
            "⭐ Легенда о герое: {}",
            "✨ Пророчество: {}"
        )
        
        // Извлекаем суть (первые 3 слова)
        val essence = title.split(" ").take(3).joinToString(" ")
        val templateIndex = (title.hashCode() % titleTemplates.size).let { if (it < 0) it + titleTemplates.size else it }
        val fantasyTitle = titleTemplates[templateIndex].replace("{}", essence)
        
        // Генерация описания
        val difficultyName = when (difficulty) {
            1 -> "простой"
            2 -> "лёгкий"
            3 -> "средний"
            4 -> "сложный"
            5 -> "легендарный"
            else -> "неизвестный"
        }
        
        val fantasyDescription = """
            В мистическом царстве продуктивности ждёт великое испытание. 
            Древние свитки гласят о «${title.lowercase()}». 
            
            Только герой твоего калибра может взяться за этот $difficultyName квест. 
            Королевство зависит от твоего успеха, отважный искатель приключений!
            
            ${if (description.isNotBlank()) "📜 Детали: $description" else ""}
        """.trimIndent()
        
        return Pair(fantasyTitle, fantasyDescription)
    }
    
    /**
     * 🔄 Переключение между фэнтези и оригинальной версией
     */
    fun toggleFantasyVersion(taskId: Int) {
        val current = _uiState.value.tasks.toMutableList()
        val idx = current.indexOfFirst { it.id == taskId }
        if (idx != -1) {
            val task = current[idx]
            current[idx] = task.copy(showFantasyVersion = !task.showFantasyVersion)
            _uiState.value = _uiState.value.copy(tasks = current)
        }
    }
    
    fun toggleTask(taskId: Int) {
        viewModelScope.launch {
            try {
                val current = _uiState.value.tasks
                val task = current.find { it.id == taskId } ?: return@launch
                val wasCompleted = task.completed
                val newCompletedStatus = !task.completed
                
                // 🌐 Обновляем на сервере
                val updateRequest = com.irlquest.app.data.network.dto.UpdateTaskRequest(
                    completed = newCompletedStatus,
                    status = if (newCompletedStatus) "completed" else "pending"
                )
                
                val updatedDto = api.updateTask(taskId, updateRequest).body()
                
                if (updatedDto != null) {
                    Timber.d("TasksViewModel: Task $taskId toggled, completed=${updatedDto.completed}")
                    
                    // Если задача только что завершена - показываем награды
                    if (!wasCompleted && newCompletedStatus) {
                        val xp = task.experienceReward
                        val gold = task.difficulty * 10
                        
                        // Обновляем опыт и золото пользователя
                        authViewModel?.addExperienceAndGold(xp, gold)
                        
                        // Проверяем повышение уровня
                        val (levelUp, newLevel) = authViewModel?.checkLevelUp(xp) ?: Pair(false, null)
                        
                        // Обновляем список и показываем диалог
                        loadTasks()
                        
                        _uiState.value = _uiState.value.copy(
                            showRewardDialog = true,
                            lastCompletedTask = task,
                            leveledUp = levelUp,
                            newLevel = newLevel
                        )
                    } else {
                        // Просто обновляем список
                        loadTasks()
                    }
                } else {
                    throw Exception("Сервер вернул пустой ответ")
                }
            } catch (e: Exception) {
                Timber.e(e, "TasksViewModel: Failed to toggle task")
                _uiState.value = _uiState.value.copy(error = "Ошибка обновления: ${e.message}")
            }
        }
    }
    
    fun dismissRewardDialog() {
        _uiState.value = _uiState.value.copy(
            showRewardDialog = false, 
            lastCompletedTask = null,
            leveledUp = false,
            newLevel = null
        )
    }
    
    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            try {
                // 🌐 Удаляем на сервере
                api.deleteTask(taskId)
                
                Timber.d("TasksViewModel: Task $taskId deleted from server")
                
                // Обновляем список
                loadTasks()
            } catch (e: Exception) {
                Timber.e(e, "TasksViewModel: Failed to delete task")
                _uiState.value = _uiState.value.copy(error = "Ошибка удаления: ${e.message}")
            }
        }
    }

    fun setFilter(filter: TaskFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    private fun filterTasks(tasks: List<TaskUi>, filter: TaskFilter): List<TaskUi> {
        return when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.ACTIVE -> tasks.filter { !it.completed }
            TaskFilter.COMPLETED -> tasks.filter { it.completed }
            TaskFilter.HIGH_PRIORITY -> tasks.filter { 
                it.priority == TaskPriority.HIGH || it.priority == TaskPriority.CRITICAL 
            }
            TaskFilter.OVERDUE -> tasks.filter { it.isOverdue }
        }
    }
    
    private fun calculateTodaySummary(tasks: List<TaskUi>): TaskSummary {
        val today = dateFormatter.format(Date())
        val todayTasks = tasks.filter { task ->
            task.createdAt == today || task.completedAt == today
        }
        
        val completed = todayTasks.count { it.completed }
        val experienceGained = todayTasks.filter { it.completed }.sumOf { it.experienceReward }
        
        return TaskSummary(
            total = todayTasks.size,
            completed = completed,
            experienceGained = experienceGained
        )
    }
    
    // createMockTasks удалён - используем реальные данные с сервера
    private fun createMockTasks(): List<TaskUi> {
        val today = dateFormatter.format(Date())
        val tomorrow = dateFormatter.format(Date(System.currentTimeMillis() + 86400000))
        val yesterday = dateFormatter.format(Date(System.currentTimeMillis() - 86400000))
        
        return listOf(
            TaskUi(
                id = 1,
                title = "Изучить Compose Navigation",
                description = "Разобраться с навигацией между экранами в Jetpack Compose",
                completed = false,
                status = TaskStatus.IN_PROGRESS,
                priority = TaskPriority.HIGH,
                deadline = tomorrow,
                isOverdue = false,
                estimatedDuration = 120,
                actualDuration = null,
                difficulty = 3,
                experienceReward = 30,
                tags = listOf("обучение", "android", "compose"),
                questId = 1,
                createdAt = today,
                completedAt = null
            ),
            TaskUi(
                id = 2,
                title = "Написать тесты для ViewModel",
                description = "Покрыть тестами основной функционал TasksViewModel",
                completed = true,
                status = TaskStatus.COMPLETED,
                priority = TaskPriority.MEDIUM,
                deadline = null,
                isOverdue = false,
                estimatedDuration = 90,
                actualDuration = 85,
                difficulty = 2,
                experienceReward = 20,
                tags = listOf("тестирование", "android"),
                questId = 1,
                createdAt = yesterday,
                completedAt = today
            ),
            TaskUi(
                id = 3,
                title = "Купить продукты",
                description = "Молоко, хлеб, яблоки, курица",
                completed = false,
                status = TaskStatus.PENDING,
                priority = TaskPriority.LOW,
                deadline = today,
                isOverdue = true,
                estimatedDuration = 30,
                actualDuration = null,
                difficulty = 1,
                experienceReward = 10,
                tags = listOf("дом", "покупки"),
                questId = null,
                createdAt = yesterday,
                completedAt = null
            ),
            TaskUi(
                id = 4,
                title = "Подготовить презентацию",
                description = "Создать слайды для презентации проекта на завтра",
                completed = false,
                status = TaskStatus.PENDING,
                priority = TaskPriority.CRITICAL,
                deadline = tomorrow,
                isOverdue = false,
                estimatedDuration = 180,
                actualDuration = null,
                difficulty = 4,
                experienceReward = 50,
                tags = listOf("работа", "презентация"),
                questId = null,
                createdAt = today,
                completedAt = null
            ),
            TaskUi(
                id = 5,
                title = "Заняться спортом",
                description = "Пробежка 5 км в парке",
                completed = true,
                status = TaskStatus.COMPLETED,
                priority = TaskPriority.MEDIUM,
                deadline = null,
                isOverdue = false,
                estimatedDuration = 60,
                actualDuration = 55,
                difficulty = 2,
                experienceReward = 20,
                tags = listOf("здоровье", "спорт"),
                questId = 2,
                createdAt = today,
                completedAt = today
            ),
            TaskUi(
                id = 6,
                title = "Прочитать главу книги",
                description = "Глава 5: Архитектурные паттерны Android",
                completed = false,
                status = TaskStatus.PENDING,
                priority = TaskPriority.LOW,
                deadline = null,
                isOverdue = false,
                estimatedDuration = 45,
                actualDuration = null,
                difficulty = 2,
                experienceReward = 15,
                tags = listOf("чтение", "обучение"),
                questId = null,
                createdAt = today,
                completedAt = null
            )
        )
    }
}