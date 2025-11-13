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
    val error: String? = null,
    val showAddTaskDialog: Boolean = false,
    val showVerificationDialog: Boolean = false,
    val verification: com.irlquest.shared.models.MLVerificationResponse? = null,
    val verificationResult: String? = null
)

class QuestDetailViewModel(
    private val questRepo: QuestRepository = QuestRepository(),
    private val taskRepo: TaskRepository = TaskRepository(),
    private val mlRepo: com.irlquest.app.data.repository.MLRepository = com.irlquest.app.data.repository.MLRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuestDetailUiState())
    val uiState: StateFlow<QuestDetailUiState> = _uiState.asStateFlow()
    
    var showAddTaskDialog: Boolean
        get() = _uiState.value.showAddTaskDialog
        set(value) {
            _uiState.value = _uiState.value.copy(showAddTaskDialog = value)
        }

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

    fun addTaskToQuest(questId: Int, title: String, description: String?) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                val request = com.irlquest.app.data.network.dto.CreateTaskRequest(
                    title = title,
                    description = description,
                    priority = "medium",
                    experienceReward = 10,
                    estimatedDuration = null,
                    difficulty = 1,
                    questId = questId,
                    deadline = null,
                    tags = null
                )
                taskRepo.createTaskForQuest(request)
                // Перезагружаем квест для обновления списка задач
                loadQuest(questId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Ошибка добавления задачи")
            }
        }
    }

    fun updateQuest(
        id: Int,
        title: String? = null,
        description: String? = null,
        priority: String? = null,  // Изменено с Int? на String? для соответствия серверу
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

    /**
     * Запрос ML верификации для завершения квеста
     */
    fun requestQuestVerification(questId: Int, questTitle: String, questDescription: String?, userLevel: Int? = null) {
        viewModelScope.launch {
            try {
                val verification = mlRepo.requestVerification(
                    questId = questId,
                    questTitle = questTitle,
                    questDescription = questDescription,
                    userLevel = userLevel
                )
                _uiState.value = _uiState.value.copy(
                    showVerificationDialog = true,
                    verification = verification
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Не удалось запросить верификацию: ${e.message}"
                )
            }
        }
    }

    /**
     * Отправка ответов на тест
     */
    fun submitQuizAnswers(questId: Int, answers: List<Int>) {
        viewModelScope.launch {
            try {
                val result = mlRepo.submitQuiz(questId, answers)
                val resultMessage = if (result.passed) {
                    "✅ Тест пройден! ${result.correctCount}/${result.totalCount} правильно. ${result.feedback}"
                } else {
                    "❌ Тест не пройден. ${result.correctCount}/${result.totalCount} правильно. ${result.feedback}"
                }
                _uiState.value = _uiState.value.copy(
                    showVerificationDialog = false,
                    verificationResult = resultMessage
                )
                if (result.passed) {
                    // Помечаем квест как завершённый
                    _uiState.value.quest?.id?.let { qid ->
                        updateQuest(qid, status = "completed")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка отправки теста: ${e.message}"
                )
            }
        }
    }

    /**
     * Отправка фото для верификации
     */
    fun submitPhotoVerification(questId: Int, imageBase64: String, latitude: Double? = null, longitude: Double? = null) {
        viewModelScope.launch {
            try {
                val result = mlRepo.submitPhoto(questId, imageBase64, latitude, longitude)
                val resultMessage = if (result.approved) {
                    "✅ Фото подтверждено (${(result.aiConfidence * 100).toInt()}% уверенности). ${result.feedback}"
                } else {
                    "❌ Фото не прошло проверку. ${result.feedback}"
                }
                _uiState.value = _uiState.value.copy(
                    showVerificationDialog = false,
                    verificationResult = resultMessage
                )
                if (result.approved) {
                    // Помечаем квест как завершённый
                    _uiState.value.quest?.id?.let { qid ->
                        updateQuest(qid, status = "completed")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка отправки фото: ${e.message}"
                )
            }
        }
    }

    fun dismissVerificationDialog() {
        _uiState.value = _uiState.value.copy(
            showVerificationDialog = false,
            verification = null
        )
    }

    fun clearVerificationResult() {
        _uiState.value = _uiState.value.copy(verificationResult = null)
    }

    private fun questDtoToUi(dto: QuestDto): QuestUi {
        val status = when (dto.status?.lowercase(Locale.getDefault())) {
            "active" -> QuestStatus.ACTIVE
            "completed" -> QuestStatus.COMPLETED
            "paused" -> QuestStatus.PAUSED
            "archived" -> QuestStatus.ARCHIVED
            else -> QuestStatus.ACTIVE
        }
        val priority = when (dto.priority) {
            "low", "1" -> QuestPriority.LOW
            "high", "3" -> QuestPriority.HIGH
            "critical", "4" -> QuestPriority.CRITICAL
            else -> QuestPriority.MEDIUM
        }
        val deadline = dto.tasks.firstOrNull()?.deadline
        val isOverdue = false
        return QuestUi(
            id = dto.id,
            title = dto.title ?: "",
            description = dto.description ?: "",
            status = status,
            priority = priority,
            difficulty = dto.difficulty ?: 1,
            completionPercentage = dto.completionPercentage ?: 0,
            totalTasks = dto.tasks.size,
            completedTasks = dto.tasks.count { it.completed },
            experienceReward = dto.rewardExperience ?: 0,
            deadline = deadline,
            isOverdue = isOverdue,
            createdAt = dto.createdAt ?: "",
            questType = dto.questType ?: "personal",
            tasks = dto.tasks
        )
    }
}
