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
    private val repo: QuestRepository = QuestRepository(),
    private val mlRepo: com.irlquest.app.data.repository.MLRepository = com.irlquest.app.data.repository.MLRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuestsUiState())
    val uiState: StateFlow<QuestsUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    init {
        loadQuests()
    }

    fun loadQuests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Загружаем с сервера
                val questsDto = repo.listQuests()
                val uiList = questsDto.map { dto -> dtoToUi(dto) }
                
                timber.log.Timber.d("QuestsViewModel: Loaded ${uiList.size} quests from server")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    quests = uiList,
                    filteredQuests = filterQuests(uiList, _uiState.value.selectedFilter)
                )
            } catch (e: Exception) {
                timber.log.Timber.e(e, "QuestsViewModel: Failed to load quests")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    quests = emptyList(),
                    filteredQuests = emptyList(),
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
                // 🌐 Создаём на сервере
                val newDto = repo.createQuest(title, description, difficulty)
                val newQuest = dtoToUi(newDto)
                
                timber.log.Timber.d("QuestsViewModel: Quest created successfully, id=${newQuest.id}")
                
                // Обновляем список квестов
                loadQuests()
            } catch (e: Exception) {
                timber.log.Timber.e(e, "QuestsViewModel: Failed to create quest")
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка создания квеста: ${e.message}"
                )
            }
        }
    }

    /**
     * Создание квеста из ML-генерации
     */
    fun createQuestFromML(generated: com.irlquest.shared.models.MLQuestGenerationResponse) {
        viewModelScope.launch {
            try {
                // Создаём квест с данными от ML
                val newDto = repo.createQuest(
                    title = generated.title,
                    description = generated.description,
                    difficulty = generated.difficulty // ML автоопределил сложность
                )
                
                timber.log.Timber.d("QuestsViewModel: ML-generated quest created, id=${newDto.id}")
                
                // Обновляем список квестов
                loadQuests()
            } catch (e: Exception) {
                timber.log.Timber.e(e, "QuestsViewModel: Failed to create ML quest")
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка создания квеста: ${e.message}"
                )
            }
        }
    }

    private fun dtoToUi(dto: com.irlquest.app.data.network.dto.QuestDto): QuestUi {
        val tasks = dto.tasks
        val total = tasks.size
        val completed = tasks.count { it.completed ?: false }
        
        val status = when (dto.status?.lowercase(Locale.getDefault())) {
            "active" -> QuestStatus.ACTIVE
            "completed" -> QuestStatus.COMPLETED
            "paused" -> QuestStatus.PAUSED
            "archived" -> QuestStatus.ARCHIVED
            else -> QuestStatus.ACTIVE
        }
        
        val priority = when (dto.priority?.lowercase()) {
            "critical" -> QuestPriority.CRITICAL
            "high" -> QuestPriority.HIGH
            "medium" -> QuestPriority.MEDIUM
            "low" -> QuestPriority.LOW
            else -> QuestPriority.MEDIUM
        }
        
        val createdAt = dto.createdAt ?: ""
        
        return QuestUi(
            id = dto.id,
            title = dto.title,
            description = dto.description ?: "",
            status = status,
            priority = priority,
            difficulty = dto.difficulty ?: 1,
            completionPercentage = dto.completionPercentage ?: 0,
            totalTasks = total,
            completedTasks = completed,
            experienceReward = dto.rewardExperience ?: 0,
            deadline = null,
            isOverdue = false,
            createdAt = createdAt ?: "",
            questType = dto.questType ?: "personal",
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