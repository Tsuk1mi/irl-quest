package com.irlquest.app.feature.worldmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlquest.app.data.repository.QuestRepository
import com.irlquest.app.data.repository.GeolocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class WorldMapUiState(
    val isLoading: Boolean = false,
    val zones: List<WorldZone> = emptyList(),
    val quests: List<com.irlquest.app.data.network.dto.QuestDto> = emptyList(),
    val error: String? = null
)

class WorldMapViewModel(
    private val questRepo: QuestRepository = QuestRepository(),
    private val geoRepo: GeolocationRepository = GeolocationRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorldMapUiState())
    val uiState: StateFlow<WorldMapUiState> = _uiState.asStateFlow()

    fun loadZones() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val quests = questRepo.listQuests()
                
                // Группируем квесты по категориям/тегам для создания зон
                val workQuests = quests.filter { quest ->
                    quest.tags?.any { tag ->
                        tag.contains("работа", ignoreCase = true) || 
                        tag.contains("work", ignoreCase = true) ||
                        tag.contains("job", ignoreCase = true)
                    } == true
                }
                val studyQuests = quests.filter { quest ->
                    quest.tags?.any { tag ->
                        tag.contains("обучение", ignoreCase = true) || 
                        tag.contains("study", ignoreCase = true) ||
                        tag.contains("учеба", ignoreCase = true)
                    } == true
                }
                val healthQuests = quests.filter { quest ->
                    quest.tags?.any { tag ->
                        tag.contains("здоровье", ignoreCase = true) || 
                        tag.contains("health", ignoreCase = true) || 
                        tag.contains("спорт", ignoreCase = true) ||
                        tag.contains("sport", ignoreCase = true)
                    } == true
                }
                val hobbyQuests = quests.filter { quest ->
                    quest.tags?.any { tag ->
                        tag.contains("хобби", ignoreCase = true) || 
                        tag.contains("hobby", ignoreCase = true)
                    } == true
                }
                val overdueQuests = quests.filter { 
                    it.status == "overdue" || it.status == "pending" 
                }

                val zones = mutableListOf<WorldZone>()
                
                if (workQuests.isNotEmpty()) {
                    val completed = workQuests.count { it.status == "completed" }
                    zones.add(WorldZone(
                        id = 1,
                        name = "Город Дел",
                        description = "Место, где выполняются рабочие задачи и профессиональные квесты.",
                        category = "work",
                        icon = "",
                        completionPercentage = if (workQuests.isNotEmpty()) (completed * 100 / workQuests.size) else 0,
                        totalQuests = workQuests.size,
                        completedQuests = completed,
                        color = androidx.compose.ui.graphics.Color(0xFF1976D2)
                    ))
                }

                if (studyQuests.isNotEmpty()) {
                    val completed = studyQuests.count { it.status == "completed" }
                    zones.add(WorldZone(
                        id = 2,
                        name = "Гора Знаний",
                        description = "Священное место обучения и развития навыков.",
                        category = "study",
                        icon = "",
                        completionPercentage = if (studyQuests.isNotEmpty()) (completed * 100 / studyQuests.size) else 0,
                        totalQuests = studyQuests.size,
                        completedQuests = completed,
                        color = com.irlquest.app.ui.theme.MagicPurple
                    ))
                }

                if (hobbyQuests.isNotEmpty()) {
                    val completed = hobbyQuests.count { it.status == "completed" }
                    zones.add(WorldZone(
                        id = 3,
                        name = "Лес Спокойствия",
                        description = "Тихая локация для отдыха и хобби.",
                        category = "hobby",
                        icon = "",
                        completionPercentage = if (hobbyQuests.isNotEmpty()) (completed * 100 / hobbyQuests.size) else 0,
                        totalQuests = hobbyQuests.size,
                        completedQuests = completed,
                        color = com.irlquest.app.ui.theme.Secondary
                    ))
                }

                if (healthQuests.isNotEmpty()) {
                    val completed = healthQuests.count { it.status == "completed" }
                    zones.add(WorldZone(
                        id = 4,
                        name = "Храм Здоровья",
                        description = "Святилище, посвященное физическому и ментальному здоровью.",
                        category = "health",
                        icon = "",
                        completionPercentage = if (healthQuests.isNotEmpty()) (completed * 100 / healthQuests.size) else 0,
                        totalQuests = healthQuests.size,
                        completedQuests = completed,
                        color = androidx.compose.ui.graphics.Color(0xFFE91E63)
                    ))
                }

                if (overdueQuests.isNotEmpty()) {
                    zones.add(WorldZone(
                        id = 5,
                        name = "Пещера Хаоса",
                        description = "Темное место, где скапливаются просроченные задачи.",
                        category = "overdue",
                        icon = "",
                        completionPercentage = 0,
                        totalQuests = overdueQuests.size,
                        completedQuests = 0,
                        color = com.irlquest.app.ui.theme.Error
                    ))
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    zones = zones.ifEmpty { createDefaultZones() },
                    quests = quests
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load world map zones")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    zones = createDefaultZones(),
                    error = "Не удалось загрузить карту: ${e.message}"
                )
            }
        }
    }

    /**
     * Создает зоны по умолчанию для отображения на карте
     */
    private fun createDefaultZones(): List<WorldZone> {
        return listOf(
            WorldZone(1, "Город Дел", "Рабочие задачи", "work", "", 0, 0, 0, androidx.compose.ui.graphics.Color(0xFF1976D2)),
            WorldZone(2, "Гора Знаний", "Обучение", "study", "", 0, 0, 0, com.irlquest.app.ui.theme.MagicPurple),
            WorldZone(3, "Лес Спокойствия", "Хобби", "hobby", "", 0, 0, 0, com.irlquest.app.ui.theme.Secondary),
            WorldZone(4, "Храм Здоровья", "Здоровье", "health", "", 0, 0, 0, androidx.compose.ui.graphics.Color(0xFFE91E63))
        )
    }
}

