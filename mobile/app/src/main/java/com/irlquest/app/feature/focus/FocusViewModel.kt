package com.irlquest.app.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class FocusSessionUi(
    val id: Int,
    val taskTitle: String?,
    val durationMinutes: Int,
    val actualDurationMinutes: Int?,
    val startedAt: String,
    val endedAt: String?,
    val sessionType: String,
    val productivityRating: Int?,
    val formattedDate: String
)

data class FocusTodayStats(
    val focusSessions: Int = 0,
    val totalMinutes: Int = 0,
    val productivity: Int = 0
)

data class FocusUiState(
    val isLoading: Boolean = false,
    val sessions: List<FocusSessionUi> = emptyList(),
    val activeSession: FocusSessionUi? = null,
    val todayStats: FocusTodayStats = FocusTodayStats(),
    val error: String? = null
)

class FocusViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()
    
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    
    fun loadFocusSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // TODO: Implement actual API calls
                // val sessions = focusRepository.getFocusSessions()
                // val activeSession = focusRepository.getActiveSession()
                // val todayStats = focusRepository.getTodayStats()
                
                // Mock data for now
                val mockSessions = listOf(
                    createMockSession(1, "Изучение Kotlin", 25, 23, 4),
                    createMockSession(2, "Работа над проектом", 45, 45, 5),
                    createMockSession(3, "Чтение книги", 30, 25, 3)
                )
                
                val mockStats = FocusTodayStats(
                    focusSessions = 3,
                    totalMinutes = 93,
                    productivity = 85
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    sessions = mockSessions,
                    activeSession = null,
                    todayStats = mockStats
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки данных"
                )
            }
        }
    }
    
    fun startSession(durationMinutes: Int, taskTitle: String? = null) {
        viewModelScope.launch {
            try {
                // TODO: Implement actual API call
                // val session = focusRepository.startSession(durationMinutes, taskTitle)
                
                // Mock active session
                val activeSession = FocusSessionUi(
                    id = System.currentTimeMillis().toInt(),
                    taskTitle = taskTitle,
                    durationMinutes = durationMinutes,
                    actualDurationMinutes = null,
                    startedAt = dateFormatter.format(Date()),
                    endedAt = null,
                    sessionType = "work",
                    productivityRating = null,
                    formattedDate = "Активна"
                )
                
                _uiState.value = _uiState.value.copy(
                    activeSession = activeSession,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Ошибка запуска сессии"
                )
            }
        }
    }
    
    fun stopSession() {
        viewModelScope.launch {
            try {
                val activeSession = _uiState.value.activeSession
                if (activeSession != null) {
                    // TODO: Implement actual API call
                    // focusRepository.stopSession(activeSession.id)
                    
                    // Add stopped session to history
                    val completedSession = activeSession.copy(
                        endedAt = dateFormatter.format(Date()),
                        actualDurationMinutes = activeSession.durationMinutes,
                        formattedDate = dateFormatter.format(Date())
                    )
                    
                    val updatedSessions = listOf(completedSession) + _uiState.value.sessions
                    val updatedStats = _uiState.value.todayStats.copy(
                        focusSessions = _uiState.value.todayStats.focusSessions + 1,
                        totalMinutes = _uiState.value.todayStats.totalMinutes + activeSession.durationMinutes
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        activeSession = null,
                        sessions = updatedSessions,
                        todayStats = updatedStats,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Ошибка завершения сессии"
                )
            }
        }
    }
    
    private fun createMockSession(
        id: Int,
        taskTitle: String,
        duration: Int,
        actualDuration: Int,
        rating: Int
    ): FocusSessionUi {
        val date = Date(System.currentTimeMillis() - id * 3600000L) // Each session 1 hour earlier
        return FocusSessionUi(
            id = id,
            taskTitle = taskTitle,
            durationMinutes = duration,
            actualDurationMinutes = actualDuration,
            startedAt = dateFormatter.format(date),
            endedAt = dateFormatter.format(Date(date.time + duration * 60000L)),
            sessionType = "work",
            productivityRating = rating,
            formattedDate = dateFormatter.format(date)
        )
    }
}