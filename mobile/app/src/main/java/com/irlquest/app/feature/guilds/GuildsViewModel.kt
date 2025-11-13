package com.irlquest.app.feature.guilds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlquest.app.data.network.dto.GuildDto
import com.irlquest.app.data.repository.MultiplayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class GuildsUiState(
    val isLoading: Boolean = false,
    val guilds: List<GuildDto> = emptyList(),
    val error: String? = null
)

class GuildsViewModel(
    private val repo: MultiplayerRepository = MultiplayerRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(GuildsUiState())
    val uiState: StateFlow<GuildsUiState> = _uiState.asStateFlow()

    fun loadGuilds() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val guilds = repo.getGuilds()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    guilds = guilds
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load guilds")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки: ${e.message}"
                )
            }
        }
    }

    fun createGuild(name: String, description: String?) {
        viewModelScope.launch {
            try {
                val guild = repo.createGuild(name, description, null)
                if (guild != null) {
                    loadGuilds() // Перезагружаем список
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Не удалось создать гильдию"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create guild")
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка создания: ${e.message}"
                )
            }
        }
    }
}

