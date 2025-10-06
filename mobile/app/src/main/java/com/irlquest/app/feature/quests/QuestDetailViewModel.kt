package com.irlquest.app.feature.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlquest.app.data.network.dto.QuestDto
import com.irlquest.app.data.network.dto.UpdateQuestRequest
import com.irlquest.app.data.network.dto.TaskDto
import com.irlquest.app.data.repository.QuestRepository
import com.irlquest.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class QuestDetailUiState(
    val isLoading: Boolean = false,
    val quest: QuestUi? = null,
    val error: String? = null
)

class QuestDetailViewModel(
    private val questRepo: QuestRepository = QuestRepository(),
    private val taskRepo: TaskRepository = TaskRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuestDetailUiState())
    val uiState: StateFlow<QuestDetailUiState> = _uiState.asStateFlow()

    fun loadQuest(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val questDto = questRepo.getQuest(id)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    quest = questDto?.let { questDtoToUi(it) }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки квеста"
                )
            }
        }
    }

    fun toggleTaskCompletion(taskId: Int) {
        viewModelScope.launch {
            try {
                val task = _uiState.value.quest?.tasks?.find { it.id == taskId }
                task?.let {
                    val newCompleted = !it.completed
                    taskRepo.updateTask(it.id, title = null, description = null, completed = newCompleted)
                    // reload quest to get fresh data
                    _uiState.value.quest?.id?.let { questId -> loadQuest(questId) }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateQuest(
        id: Int,
        title: String? = null,
        description: String? = null,
        priority: Int? = null,
        status: String? = null
    ) {
        viewModelScope.launch {
            try {
                val request = UpdateQuestRequest(
                    title = title,
                    description = description,
                    priority = priority,
                    status = status
                )
                val updatedQuest = questRepo.updateQuest(id, request)
                _uiState.value = _uiState.value.copy(
                    quest = updatedQuest?.let { questDtoToUi(it) }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Ошибка обновления квеста"
                )
            }
        }
    }

    private fun questDtoToUi(dto: QuestDto): QuestUi {
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
        val deadline = dto.tasks.firstOrNull()?.deadline
        val isOverdue = false
        return QuestUi(
            id = dto.id,
            title = dto.title,
            description = dto.description,
            status = status,
            priority = priority,
            difficulty = dto.difficulty,
            completionPercentage = dto.completionPercentage,
            totalTasks = dto.tasks.size,
            completedTasks = dto.tasks.count { it.completed },
            experienceReward = dto.experienceReward,
            deadline = deadline,
            isOverdue = isOverdue,
            createdAt = dto.createdAt,
            questType = dto.questType,
            tasks = dto.tasks
        )
    }
}
