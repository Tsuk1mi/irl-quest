package com.irlquest.app.feature.coop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlquest.app.data.network.dto.CoopMissionDto
import com.irlquest.app.data.repository.MultiplayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class CoopMissionsUiState(
    val isLoading: Boolean = false,
    val missions: List<CoopMissionDto> = emptyList(),
    val error: String? = null
)

class CoopMissionsViewModel(
    private val repo: MultiplayerRepository = MultiplayerRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CoopMissionsUiState())
    val uiState: StateFlow<CoopMissionsUiState> = _uiState.asStateFlow()

    fun loadMissions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val missions = repo.getCoopMissions()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    missions = missions
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load coop missions")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки: ${e.message}"
                )
            }
        }
    }

    fun joinMission(missionId: Int, role: String) {
        viewModelScope.launch {
            try {
                val success = repo.joinCoopMission(missionId, role)
                if (success) {
                    loadMissions() // Перезагружаем список
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Не удалось присоединиться к миссии"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to join mission")
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка: ${e.message}"
                )
            }
        }
    }
}

