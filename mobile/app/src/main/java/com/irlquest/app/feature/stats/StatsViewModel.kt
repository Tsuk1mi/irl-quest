package com.irlquest.app.feature.stats

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlquest.app.ui.theme.*
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
    val focusTimeStats: List<DayData> = emptyList(),
    val levelProgress: Float = 0f,
    val currentLevel: Int = 1,
    val currentExperience: Int = 0,
    val nextLevelExperience: Int = 1000,
    val error: String? = null
)

data class StatsData(
    val levelProgress: Float,
    val currentLevel: Int,
    val currentExperience: Int,
    val nextLevelExperience: Int,
    val focusTimeStats: List<DayData>,
    val todayStats: TodayStats,
    val weeklyData: List<DayData>
)

class StatsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val achievementColors = listOf(
        Primary,      // Золото
        MysticBlue,   // Синий мистический
        Success,      // Зелёный
        Color(0xFF018786), // Secondary variant
        Color(0xFFB00020)  // Error
    )

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Имитация загрузки данных
                val mockData = createMockData()
                val mockStats = createMockStats()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userProfile = mockData.first,
                    todayStats = mockData.second,
                    weeklyData = mockStats.weeklyData,
                    achievements = createMockAchievements(),
                    activityData = createMockActivityData(),
                    focusTimeStats = mockStats.focusTimeStats,
                    levelProgress = mockStats.levelProgress,
                    currentLevel = mockStats.currentLevel,
                    currentExperience = mockStats.currentExperience,
                    nextLevelExperience = mockStats.nextLevelExperience
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun createMockData(): Pair<UserProfile, TodayStats> {
        val level = 5
        val currentExp = 750
        val nextLevelExp = 1000

        return Pair(
            UserProfile(
                username = "Максим",
                level = level,
                experience = currentExp,
                nextLevelExperience = nextLevelExp,
                experienceProgress = currentExp.toFloat() / nextLevelExp
            ),
            TodayStats(
                completedTasks = 5,
                focusMinutes = 120,
                experienceGained = 150,
                productivityScore = 85
            )
        )
    }

    private fun createMockWeeklyData(): List<DayData> {
        val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        return days.map { day ->
            DayData(
                dayName = day,
                value = Random.nextFloat() * 100
            )
        }
    }

    private fun createMockAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "1",
                name = "Первые шаги",
                emoji = "🎯",
                description = "Выполнить первую задачу",
                isUnlocked = true,
                color = achievementColors[0]
            ),
            Achievement(
                id = "2",
                name = "Фокус",
                emoji = "⏱️",
                description = "30 минут концентрации",
                isUnlocked = true,
                color = achievementColors[1]
            ),
            Achievement(
                id = "3",
                name = "Мастер",
                emoji = "🌟",
                description = "Достичь 5 уровня",
                isUnlocked = false,
                color = achievementColors[2]
            )
        )
    }

    private fun createMockActivityData(): List<ActivityDay> {
        return List(30) { index ->
            ActivityDay(
                date = "2024-${Random.nextInt(1, 13)}-${Random.nextInt(1, 29)}",
                intensity = Random.nextInt(0, 5)
            )
        }
    }

    private fun createMockFocusTimeData(): List<DayData> {
        val days = List(7) { index ->
            DayData(
                dayName = when (index) {
                    0 -> "Пн"
                    1 -> "Вт"
                    2 -> "Ср"
                    3 -> "Чт"
                    4 -> "Пт"
                    5 -> "Сб"
                    else -> "Вс"
                },
                value = Random.nextFloat() * 180 // минут
            )
        }
        return days
    }

    private fun createMockStats(): StatsData {
        return StatsData(
            levelProgress = 0.75f,
            currentLevel = 5,
            currentExperience = 750,
            nextLevelExperience = 1000,
            focusTimeStats = createMockFocusTimeData(),
            todayStats = TodayStats(5, 120, 150, 85),
            weeklyData = createMockWeeklyData()
        )
    }
}