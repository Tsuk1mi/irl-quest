package com.irlquest.app.feature.quests

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlquest.app.data.repository.QuestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class QuestsUiState(
    val isLoading: Boolean = false,
    val quests: List<QuestUi> = emptyList(),
    val filteredQuests: List<QuestUi> = emptyList(),
    val selectedFilter: QuestFilter = QuestFilter.ALL,
    val error: String? = null
)

class QuestsViewModel(
    private val repo: QuestRepository = QuestRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuestsUiState())
    val uiState: StateFlow<QuestsUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun loadQuests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val questsDto = repo.listQuests()
                val uiList = questsDto.map { dto -> dtoToUi(dto) }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    quests = uiList,
                    filteredQuests = filterQuests(uiList, _uiState.value.selectedFilter)
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
                val newDto = repo.createQuest(title, description, difficulty)
                val newQuest = dtoToUi(newDto)
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

    private fun dtoToUi(dto: com.irlquest.app.data.network.dto.QuestDto): QuestUi {
        val tasks = dto.tasks
        val total = tasks.size
        val completed = tasks.count { it.completed }
        val status = when (dto.status.lowercase(Locale.getDefault())) {
            "active" -> QuestStatus.ACTIVE
            "completed" -> QuestStatus.COMPLETED
            "paused" -> QuestStatus.PAUSED
            "archived" -> QuestStatus.ARCHIVED
            else -> QuestStatus.ACTIVE
        }
        val priority = when (dto.priority) {
            1 -> QuestPriority.LOW
            2 -> QuestPriority.MEDIUM
            3 -> QuestPriority.HIGH
            4 -> QuestPriority.CRITICAL
            else -> QuestPriority.MEDIUM
        }
        val createdAt = dto.createdAt
        return QuestUi(
            id = dto.id,
            title = dto.title,
            description = dto.description ?: "",
            status = status,
            priority = priority,
            difficulty = dto.difficulty,
            completionPercentage = dto.completionPercentage,
            totalTasks = total,
            completedTasks = completed,
            experienceReward = dto.experienceReward,
            deadline = null,
            isOverdue = false,
            createdAt = createdAt,
            questType = dto.questType,
            tasks = dto.tasks
        )
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
}