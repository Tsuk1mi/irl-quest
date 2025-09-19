package com.irlquest.app.feature.quests

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

enum class QuestStatus {
    ACTIVE, COMPLETED, PAUSED, ARCHIVED
}

enum class QuestPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class QuestFilter(val displayName: String, val icon: ImageVector) {
    ALL("Все", Icons.Default.List),
    ACTIVE("Активные", Icons.Default.PlayArrow),
    COMPLETED("Завершенные", Icons.Default.CheckCircle),
    HIGH_PRIORITY("Важные", Icons.Default.PriorityHigh),
    OVERDUE("Просроченные", Icons.Default.Warning)
}

data class QuestUi(
    val id: Int,
    val title: String,
    val description: String,
    val status: QuestStatus,
    val priority: QuestPriority,
    val difficulty: Int, // 1-5
    val completionPercentage: Int,
    val totalTasks: Int,
    val completedTasks: Int,
    val rewardExperience: Int,
    val deadline: String?,
    val isOverdue: Boolean,
    val createdAt: String,
    val questType: String
)

data class QuestsUiState(
    val isLoading: Boolean = false,
    val quests: List<QuestUi> = emptyList(),
    val filteredQuests: List<QuestUi> = emptyList(),
    val selectedFilter: QuestFilter = QuestFilter.ALL,
    val error: String? = null
)

class QuestsViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(QuestsUiState())
    val uiState: StateFlow<QuestsUiState> = _uiState.asStateFlow()
    
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    fun loadQuests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // TODO: Implement actual API calls
                // val quests = questRepository.getQuests()
                
                // Mock data for now
                val mockQuests = createMockQuests()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    quests = mockQuests,
                    filteredQuests = filterQuests(mockQuests, _uiState.value.selectedFilter)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки квестов"
                )
            }
        }
    }
    
    fun setFilter(filter: QuestFilter) {
        val currentQuests = _uiState.value.quests
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredQuests = filterQuests(currentQuests, filter)
        )
    }
    
    fun createQuest(title: String, description: String, difficulty: Int) {
        viewModelScope.launch {
            try {
                // TODO: Implement actual API call
                // val newQuest = questRepository.createQuest(title, description, difficulty)
                
                // Mock creation
                val newQuest = QuestUi(
                    id = System.currentTimeMillis().toInt(),
                    title = title,
                    description = description,
                    status = QuestStatus.ACTIVE,
                    priority = QuestPriority.MEDIUM,
                    difficulty = difficulty,
                    completionPercentage = 0,
                    totalTasks = 0,
                    completedTasks = 0,
                    rewardExperience = difficulty * 100,
                    deadline = null,
                    isOverdue = false,
                    createdAt = dateFormatter.format(Date()),
                    questType = "personal"
                )
                
                val updatedQuests = listOf(newQuest) + _uiState.value.quests
                _uiState.value = _uiState.value.copy(
                    quests = updatedQuests,
                    filteredQuests = filterQuests(updatedQuests, _uiState.value.selectedFilter)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Ошибка создания квеста"
                )
            }
        }
    }
    
    private fun filterQuests(quests: List<QuestUi>, filter: QuestFilter): List<QuestUi> {
        return when (filter) {
            QuestFilter.ALL -> quests
            QuestFilter.ACTIVE -> quests.filter { it.status == QuestStatus.ACTIVE }
            QuestFilter.COMPLETED -> quests.filter { it.status == QuestStatus.COMPLETED }
            QuestFilter.HIGH_PRIORITY -> quests.filter { 
                it.priority == QuestPriority.HIGH || it.priority == QuestPriority.CRITICAL 
            }
            QuestFilter.OVERDUE -> quests.filter { it.isOverdue }
        }
    }
    
    private fun createMockQuests(): List<QuestUi> {
        return listOf(
            QuestUi(
                id = 1,
                title = "Изучение Kotlin",
                description = "Освоить основы программирования на Kotlin для Android разработки",
                status = QuestStatus.ACTIVE,
                priority = QuestPriority.HIGH,
                difficulty = 4,
                completionPercentage = 75,
                totalTasks = 8,
                completedTasks = 6,
                rewardExperience = 400,
                deadline = "30.09.2024",
                isOverdue = false,
                createdAt = "15.09.2024",
                questType = "learning"
            ),
            QuestUi(
                id = 2,
                title = "Здоровый образ жизни",
                description = "Выработать привычки здорового питания и регулярных тренировок",
                status = QuestStatus.ACTIVE,
                priority = QuestPriority.MEDIUM,
                difficulty = 3,
                completionPercentage = 45,
                totalTasks = 10,
                completedTasks = 4,
                rewardExperience = 300,
                deadline = "31.12.2024",
                isOverdue = false,
                createdAt = "01.09.2024",
                questType = "personal"
            ),
            QuestUi(
                id = 3,
                title = "Запуск стартапа",
                description = "Разработать и запустить собственный IT-проект",
                status = QuestStatus.ACTIVE,
                priority = QuestPriority.CRITICAL,
                difficulty = 5,
                completionPercentage = 20,
                totalTasks = 15,
                completedTasks = 3,
                rewardExperience = 500,
                deadline = "15.09.2024",
                isOverdue = true,
                createdAt = "01.08.2024",
                questType = "challenge"
            ),
            QuestUi(
                id = 4,
                title = "Подготовка к марафону",
                description = "Тренировки для участия в беговом марафоне 42.2 км",
                status = QuestStatus.COMPLETED,
                priority = QuestPriority.MEDIUM,
                difficulty = 4,
                completionPercentage = 100,
                totalTasks = 12,
                completedTasks = 12,
                rewardExperience = 400,
                deadline = null,
                isOverdue = false,
                createdAt = "01.06.2024",
                questType = "personal"
            ),
            QuestUi(
                id = 5,
                title = "Изучение иностранного языка",
                description = "Достичь уровня B2 в английском языке",
                status = QuestStatus.PAUSED,
                priority = QuestPriority.LOW,
                difficulty = 3,
                completionPercentage = 60,
                totalTasks = 20,
                completedTasks = 12,
                rewardExperience = 300,
                deadline = "30.11.2024",
                isOverdue = false,
                createdAt = "15.07.2024",
                questType = "learning"
            ),
            QuestUi(
                id = 6,
                title = "Домашний ремонт",
                description = "Завершить ремонт кухни и гостиной",
                status = QuestStatus.ACTIVE,
                priority = QuestPriority.HIGH,
                difficulty = 2,
                completionPercentage = 85,
                totalTasks = 6,
                completedTasks = 5,
                rewardExperience = 200,
                deadline = "25.09.2024",
                isOverdue = false,
                createdAt = "10.09.2024",
                questType = "personal"
            )
        )
    }
}