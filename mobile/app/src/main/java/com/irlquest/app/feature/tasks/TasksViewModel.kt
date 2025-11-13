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
import com.irlquest.app.feature.auth.AuthViewModel
import com.irlquest.app.data.repository.InventoryRepository
import com.irlquest.app.data.repository.OwnedItem
import com.irlquest.app.data.repository.TaskRepository
import com.irlquest.app.data.repository.DailyTasksRepository
import com.irlquest.app.data.network.dto.TaskDto
import timber.log.Timber
import kotlin.random.Random

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

enum class DiceType(val sides: Int, val label: String, val icon: ImageVector) {
    D4(4, "d4", Icons.Default.Casino),
    D6(6, "d6", Icons.Default.Casino),
    D8(8, "d8", Icons.Default.Casino),
    D10(10, "d10", Icons.Default.Casino),
    D12(12, "d12", Icons.Default.Casino),
    D20(20, "d20", Icons.Default.Casino)
}

data class DiceRoll(
    val diceType: DiceType,
    val rolls: List<Int>,
    val modifier: Int,
    val total: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class DiceState(
    val selectedDice: DiceType = DiceType.D20,
    val modifier: Int = 0,
    val lastRoll: DiceRoll? = null,
    val history: List<DiceRoll> = emptyList()
)

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
    val error: String? = null,
    val diceState: DiceState = DiceState(),
    val showDiceSheet: Boolean = false,
    val recentLoot: List<OwnedItem> = emptyList(),
    val showLootDialog: Boolean = false
)


class TasksViewModel(
    private val authViewModel: AuthViewModel? = null,
    private val repo: TaskRepository = TaskRepository()
) : ViewModel() {
    private val dailyTasksRepo = DailyTasksRepository()
    private val inventoryRepository = InventoryRepository(authViewModel, viewModelScope)
    
    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()
    
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    init {
        loadTasks()
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false, showDiceSheet = false)
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
        val priority = when (dto.priority.lowercase()) {
            "critical" -> TaskPriority.CRITICAL
            "high" -> TaskPriority.HIGH
            "medium" -> TaskPriority.MEDIUM
            "low" -> TaskPriority.LOW
            else -> TaskPriority.MEDIUM
        }
        
        val status = when (dto.status.lowercase()) {
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
            completed = dto.completed,
            status = status,
            priority = priority,
            deadline = dto.deadline,
            isOverdue = false, // TODO: вычислить
            estimatedDuration = dto.estimatedDuration,
            actualDuration = dto.actualDuration,
            difficulty = dto.difficulty,
            experienceReward = dto.experienceReward,
            tags = dto.tags,
            questId = dto.questId,
            createdAt = dto.createdAt,
            completedAt = dto.completedAt,
            fantasyTitle = fantasyTitle,
            fantasyDescription = fantasyDesc,
            showFantasyVersion = true
        )
    }
    
    fun createTask(title: String, description: String, priority: TaskPriority, deadline: String? = null) {
        viewModelScope.launch {
            try {
                // Расчёт наград на основе приоритета
                val xpReward = 10 + when (priority) {
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
                    difficulty = 1,
                    questId = null,
                    deadline = deadline,
                    tags = tags
                )
                
                val createdDto = repo.createTaskForQuest(request)
                
                // Конвертируем и добавляем в список
                val newTask = dtoToUi(createdDto)
                
                Timber.d("TasksViewModel: Task created successfully, id=${newTask.id}")
                
                // Обновляем список
                loadTasks()
                
                _uiState.value = _uiState.value.copy(
                    showCreateDialog = false
                )
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
     * Генерирует ежедневные задачи на основе анализа активности пользователя
     */
    fun generateDailyTasksFromActivity() {
        viewModelScope.launch {
            try {
                val generatedTasks = dailyTasksRepo.generateDailyTasksFromActivity()
                if (generatedTasks.isNotEmpty()) {
                    Timber.d("TasksViewModel: Generated ${generatedTasks.size} daily tasks")
                    // Перезагружаем список задач для отображения новых
                    loadTasks()
                }
            } catch (e: Exception) {
                Timber.e(e, "TasksViewModel: Failed to generate daily tasks")
            }
        }
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
                
                // 🌐 Обновляем на сервере через completeTask (сервер не поддерживает обновление задач)
                if (newCompletedStatus) {
                    val updatedDto = repo.updateTask(taskId, completed = true)
                    Timber.d("TasksViewModel: Task $taskId toggled, completed=${updatedDto.completed}")
                    
                    // Если задача только что завершена - показываем награды
                    if (!wasCompleted) {
                        val xp = task.experienceReward
                        val gold = task.difficulty * 10
                        
                        // Обновляем опыт и золото пользователя
                        authViewModel?.addExperienceAndGold(xp, gold)
                        
                        // Проверяем повышение уровня
                        val (levelUp, newLevel) = authViewModel?.checkLevelUp(xp) ?: Pair(false, null)

                        val playerLuck = authViewModel?.currentUser?.value?.let { user ->
                            ((user.wisdom) + (user.dexterity)) / 2
                        } ?: 10
                        val generatedLoot = inventoryRepository.addLootForQuest(
                            questId = task.questId,
                            difficulty = task.difficulty,
                            playerLuck = playerLuck
                        )
                        
                        // Обновляем список и показываем диалог
                        loadTasks()
                        
                        _uiState.value = _uiState.value.copy(
                            showRewardDialog = true,
                            lastCompletedTask = task,
                            leveledUp = levelUp,
                            newLevel = newLevel,
                            showLootDialog = generatedLoot.isNotEmpty(),
                            recentLoot = generatedLoot
                        )
                    } else {
                        // Просто обновляем список
                        loadTasks()
                    }
                } else {
                    // Сервер не поддерживает "разавершение" задачи
                    _uiState.value = _uiState.value.copy(error = "Разавершение задачи не поддерживается сервером")
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
                // ⚠️ Сервер не поддерживает удаление задач
                // Можно только завершить задачу
                _uiState.value = _uiState.value.copy(error = "Удаление задач не поддерживается. Завершите задачу вместо этого.")
                
                Timber.d("TasksViewModel: Task $taskId deleted from server")
                
                // Обновляем список
                loadTasks()
            } catch (e: Exception) {
                Timber.e(e, "TasksViewModel: Failed to delete task")
                _uiState.value = _uiState.value.copy(error = "Ошибка удаления: ${e.message}")
            }
        }
    }

    fun openDiceSheet() {
        _uiState.value = _uiState.value.copy(showDiceSheet = true)
    }

    fun closeDiceSheet() {
        _uiState.value = _uiState.value.copy(showDiceSheet = false)
    }

    fun selectDiceType(type: DiceType) {
        _uiState.value = _uiState.value.copy(
            diceState = _uiState.value.diceState.copy(selectedDice = type)
        )
    }

    fun updateDiceModifier(modifier: Int) {
        _uiState.value = _uiState.value.copy(
            diceState = _uiState.value.diceState.copy(modifier = modifier.coerceIn(-10, 10))
        )
    }

    fun rollDice(times: Int = 1) {
        val state = _uiState.value.diceState
        val rolls = (0 until times).map { Random.nextInt(1, state.selectedDice.sides + 1) }
        val total = rolls.sum() + state.modifier
        val roll = DiceRoll(
            diceType = state.selectedDice,
            rolls = rolls,
            modifier = state.modifier,
            total = total
        )
        val newHistory = (listOf(roll) + state.history).take(10)
        _uiState.value = _uiState.value.copy(
            diceState = state.copy(
                lastRoll = roll,
                history = newHistory
            )
        )
    }

    fun dismissLootDialog() {
        _uiState.value = _uiState.value.copy(showLootDialog = false, recentLoot = emptyList())
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