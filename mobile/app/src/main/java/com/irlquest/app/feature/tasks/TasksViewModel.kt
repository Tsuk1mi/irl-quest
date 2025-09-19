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
    val total: Int = 0,
    val completed: Int = 0,
    val experienceGained: Int = 0
)

data class TasksUiState(
    val isLoading: Boolean = false,
    val tasks: List<TaskUi> = emptyList(),
    val filteredTasks: List<TaskUi> = emptyList(),
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val todaySummary: TaskSummary = TaskSummary(),
    val error: String? = null
)

class TasksViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()
    
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // TODO: Implement actual API calls
                // val tasks = taskRepository.getTasks()
                
                // Mock data for now
                val mockTasks = createMockTasks()
                val todaySummary = calculateTodaySummary(mockTasks)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    tasks = mockTasks,
                    filteredTasks = filterTasks(mockTasks, _uiState.value.selectedFilter),
                    todaySummary = todaySummary
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки задач"
                )
            }
        }
    }
    
    fun setFilter(filter: TaskFilter) {
        val currentTasks = _uiState.value.tasks
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredTasks = filterTasks(currentTasks, filter)
        )
    }
    
    fun toggleTask(taskId: Int) {
        viewModelScope.launch {
            try {
                // TODO: Implement actual API call
                // taskRepository.toggleTask(taskId)
                
                val updatedTasks = _uiState.value.tasks.map { task ->
                    if (task.id == taskId) {
                        task.copy(
                            completed = !task.completed,
                            status = if (!task.completed) TaskStatus.COMPLETED else TaskStatus.PENDING,
                            completedAt = if (!task.completed) dateFormatter.format(Date()) else null
                        )
                    } else task
                }
                
                val todaySummary = calculateTodaySummary(updatedTasks)
                
                _uiState.value = _uiState.value.copy(
                    tasks = updatedTasks,
                    filteredTasks = filterTasks(updatedTasks, _uiState.value.selectedFilter),
                    todaySummary = todaySummary,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Ошибка обновления задачи"
                )
            }
        }
    }
    
    fun createTask(title: String, description: String, priority: TaskPriority) {
        viewModelScope.launch {
            try {
                // TODO: Implement actual API call
                // val newTask = taskRepository.createTask(title, description, priority)
                
                // Mock creation
                val newTask = TaskUi(
                    id = System.currentTimeMillis().toInt(),
                    title = title,
                    description = description,
                    completed = false,
                    status = TaskStatus.PENDING,
                    priority = priority,
                    deadline = null,
                    isOverdue = false,
                    estimatedDuration = null,
                    actualDuration = null,
                    difficulty = when (priority) {
                        TaskPriority.LOW -> 1
                        TaskPriority.MEDIUM -> 2
                        TaskPriority.HIGH -> 3
                        TaskPriority.CRITICAL -> 4
                    },
                    experienceReward = when (priority) {
                        TaskPriority.LOW -> 10
                        TaskPriority.MEDIUM -> 20
                        TaskPriority.HIGH -> 30
                        TaskPriority.CRITICAL -> 50
                    },
                    tags = emptyList(),
                    questId = null,
                    createdAt = dateFormatter.format(Date()),
                    completedAt = null
                )
                
                val updatedTasks = listOf(newTask) + _uiState.value.tasks
                val todaySummary = calculateTodaySummary(updatedTasks)
                
                _uiState.value = _uiState.value.copy(
                    tasks = updatedTasks,
                    filteredTasks = filterTasks(updatedTasks, _uiState.value.selectedFilter),
                    todaySummary = todaySummary,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Ошибка создания задачи"
                )
            }
        }
    }
    
    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            try {
                // TODO: Implement actual API call
                // taskRepository.deleteTask(taskId)
                
                val updatedTasks = _uiState.value.tasks.filter { it.id != taskId }
                val todaySummary = calculateTodaySummary(updatedTasks)
                
                _uiState.value = _uiState.value.copy(
                    tasks = updatedTasks,
                    filteredTasks = filterTasks(updatedTasks, _uiState.value.selectedFilter),
                    todaySummary = todaySummary,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Ошибка удаления задачи"
                )
            }
        }
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