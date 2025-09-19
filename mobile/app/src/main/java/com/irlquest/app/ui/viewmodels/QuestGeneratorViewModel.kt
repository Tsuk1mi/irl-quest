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
    val generatedQuest: QuestGenerationResponse? = null,
    val error: String? = null
)

class QuestGeneratorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(QuestGeneratorUiState())
    val uiState: StateFlow<QuestGeneratorUiState> = _uiState.asStateFlow()
    
    private val apiService = RetrofitClient.apiService

    suspend fun generateQuest(request: QuestGenerationRequest) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )
        
        viewModelScope.launch {
            try {
                val response = apiService.generateQuest(request)
                
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generatedQuest = response.body(),
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to generate quest: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearGeneratedQuest() {
        _uiState.value = _uiState.value.copy(generatedQuest = null)
    }
}