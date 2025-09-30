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
    val completedAt: String?
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
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val todaySummary: TaskSummary = TaskSummary(0, 0, 0),
    val error: String? = null
)


class TasksViewModel : ViewModel() {
    
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
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // TODO: Загрузка задач с сервера
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    tasks = emptyList() // Временно пустой список
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun createTask(title: String, description: String, priority: TaskPriority) {
        viewModelScope.launch {
            try {
                // TODO: Создание задачи на сервере
                hideCreateDialog()
                loadTasks()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun toggleTask(taskId: Int) {
        viewModelScope.launch {
            try {
                // TODO: Обновление статуса задачи на сервере
                loadTasks()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            try {
                // TODO: Удаление задачи на сервере
                loadTasks()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
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