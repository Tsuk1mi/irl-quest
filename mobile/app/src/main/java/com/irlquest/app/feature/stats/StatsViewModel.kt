package com.irlquest.app.feature.stats

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class UserProfile(
    val username: String,
    val level: Int,
    val experience: Int,
    val nextLevelExperience: Int,
    val experienceProgress: Float
)

data class TodayStats(
    val completedTasks: Int = 0,
    val focusMinutes: Int = 0,
    val experienceGained: Int = 0,
    val productivityScore: Int = 0
)

data class DayData(
    val dayName: String,
    val value: Float
)

data class Achievement(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val isUnlocked: Boolean,
    val color: Color
)

data class ActivityDay(
    val date: String,
    val intensity: Int // 0-4
)

data class StatsUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfile = UserProfile("", 1, 0, 1000, 0f),
    val todayStats: TodayStats = TodayStats(),
    val weeklyData: List<DayData> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val activityData: List<ActivityDay> = emptyList(),
    val error: String? = null
)

class StatsViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()
    
    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // TODO: Implement actual API calls
                // val userProfile = userRepository.getCurrentUser()
                // val todayStats = statsRepository.getTodayStats()
                // val weeklyData = statsRepository.getWeeklyStats()
                // val achievements = achievementsRepository.getUserAchievements()
                // val activityData = statsRepository.getActivityData()
                
                // Mock data for now
                val mockUserProfile = UserProfile(
                    username = "IRL Quest Hero",
                    level = 7,
                    experience = 1420,
                    nextLevelExperience = 2000,
                    experienceProgress = 0.71f
                )
                
                val mockTodayStats = TodayStats(
                    completedTasks = 5,
                    focusMinutes = 120,
                    experienceGained = 85,
                    productivityScore = 92
                )
                
                val mockWeeklyData = listOf(
                    DayData("Пн", 45f),
                    DayData("Вт", 60f),
                    DayData("Ср", 30f),
                    DayData("Чт", 75f),
                    DayData("Пт", 90f),
                    DayData("Сб", 20f),
                    DayData("Вс", 15f)
                )
                
                val mockAchievements = listOf(
                    Achievement("first_task", "Первая задача", "🎯", "Выполни свою первую задачу", true, Color.Green),
                    Achievement("focus_master", "Мастер фокуса", "🧠", "Проведи 10 фокус-сессий", true, Color.Blue),
                    Achievement("week_streak", "Недельная серия", "🔥", "Выполняй задачи 7 дней подряд", false, Color.Orange),
                    Achievement("early_bird", "Ранняя пташка", "🐣", "Начни задачу до 8 утра", true, Color.Yellow),
                    Achievement("night_owl", "Сова", "🦉", "Выполни задачу после 22:00", false, Color.Purple),
                    Achievement("quest_master", "Мастер квестов", "👑", "Заверши 5 квестов", false, Color.Red)
                )
                
                val mockActivityData = generateMockActivityData()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userProfile = mockUserProfile,
                    todayStats = mockTodayStats,
                    weeklyData = mockWeeklyData,
                    achievements = mockAchievements,
                    activityData = mockActivityData
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки статистики"
                )
            }
        }
    }
    
    private fun generateMockActivityData(): List<ActivityDay> {
        return (0..29).map { dayOffset ->
            ActivityDay(
                date = "2024-${12 - dayOffset / 30}-${(dayOffset % 30) + 1}",
                intensity = Random.nextInt(0, 5)
            )
        }.reversed()
    }
}