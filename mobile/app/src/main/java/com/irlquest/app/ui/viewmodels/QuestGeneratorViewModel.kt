package com.irlquest.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlquest.app.data.network.RetrofitClient
import com.irlquest.app.data.network.dto.QuestGenerationRequest
import com.irlquest.app.data.network.dto.QuestGenerationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuestGeneratorUiState(
    val isLoading: Boolean = false,
    val quest: QuestGenerationResponse? = null,
    val error: String? = null
)

class QuestGeneratorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(QuestGeneratorUiState())
    val uiState: StateFlow<QuestGeneratorUiState> = _uiState.asStateFlow()
    
    private val apiService = RetrofitClient.apiService

    fun generateQuest(request: QuestGenerationRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val response = apiService.generateQuest(request)
                
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        quest = response.body(),
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не удалось сгенерировать квест: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка сети: ${e.message}"
                )
            }
        }
    }
}