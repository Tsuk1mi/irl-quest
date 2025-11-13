package com.irlquest.app.feature.stats

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlquest.app.data.repository.StatsRepository
import com.irlquest.app.data.repository.CharacterRepository
import com.irlquest.app.data.repository.TaskRepository
import com.irlquest.app.data.repository.QuestRepository
import com.irlquest.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*
import timber.log.Timber

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

class StatsViewModel(
    private val statsRepo: StatsRepository = StatsRepository(),
    private val characterRepo: CharacterRepository = CharacterRepository(),
    private val taskRepo: TaskRepository = TaskRepository(),
    private val questRepo: QuestRepository = QuestRepository()
) : ViewModel() {
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
                // Загружаем реальные данные с сервера
                val totalStats = statsRepo.getTotalStats()
                val dailyStats = statsRepo.getDailyStats()
                val characterProfile = characterRepo.getCharacterProfile()
                val tasks = taskRepo.listTasks()
                val quests = questRepo.listQuests()

                // Сегодняшняя статистика
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val todayStatsData = dailyStats.find { it.date == today }
                
                // Подсчитываем выполненные задачи за сегодня
                val todayCompletedTasks = tasks.count { task ->
                    task.completed && task.completedAt?.startsWith(today) == true
                }
                
                val todayStats = TodayStats(
                    completedTasks = todayStatsData?.tasksCompleted ?: todayCompletedTasks,
                    focusMinutes = todayStatsData?.focusTime ?: 0,
                    experienceGained = todayStatsData?.experienceGained ?: 0,
                    productivityScore = if (todayStatsData != null && todayStatsData.tasksTotal > 0) {
                        (todayStatsData.tasksCompleted * 100 / todayStatsData.tasksTotal).coerceIn(0, 100)
                    } else if (tasks.isNotEmpty()) {
                        (todayCompletedTasks * 100 / tasks.size).coerceIn(0, 100)
                    } else 0
                )

                // Профиль пользователя
                val character = characterProfile?.character
                val totalStatsData = totalStats
                
                // Используем данные из totalStats, если доступны, иначе из character
                val level = totalStatsData?.currentLevel ?: character?.level ?: 1
                val experience = totalStatsData?.totalExperience ?: character?.experience ?: 0
                val nextLevelExp = totalStatsData?.nextLevelExperience ?: characterProfile?.experienceToNextLevel ?: (level * 100)
                
                // Правильный расчет прогресса: текущий опыт относительно опыта для следующего уровня
                val levelProgress = if (nextLevelExp > 0) {
                    // Если у нас есть totalExperience, нужно вычислить опыт текущего уровня
                    val currentLevelExp = if (level > 1) {
                        // Опыт для предыдущего уровня (упрощенная формула: каждый уровень требует level * 100)
                        (level - 1) * 100
                    } else 0
                    val expInCurrentLevel = experience - currentLevelExp
                    val expNeededForNextLevel = nextLevelExp - currentLevelExp
                    if (expNeededForNextLevel > 0) (expInCurrentLevel.toFloat() / expNeededForNextLevel).coerceIn(0f, 1f) else 0f
                } else 0f

                val userProfile = UserProfile(
                    username = characterProfile?.character?.characterClass ?: "Герой",
                    level = level,
                    experience = experience,
                    nextLevelExperience = nextLevelExp,
                    experienceProgress = levelProgress
                )

                // Недельная статистика
                val weeklyData = dailyStats.takeLast(7).mapIndexed { index, stat ->
                    DayData(
                        dayName = getDayName(index),
                        value = stat.tasksCompleted.toFloat()
                    )
                }

                // Если данных меньше 7 дней, дополняем нулями
                val fullWeeklyData = if (weeklyData.size < 7) {
                    weeklyData + List(7 - weeklyData.size) { index ->
                        DayData(getDayName(weeklyData.size + index), 0f)
                    }
                } else weeklyData

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userProfile = userProfile,
                    todayStats = todayStats,
                    weeklyData = fullWeeklyData,
                    achievements = createMockAchievements(), // TODO: загрузить с сервера
                    activityData = createActivityDataFromStats(dailyStats),
                    focusTimeStats = dailyStats.takeLast(7).mapIndexed { index, stat ->
                        DayData(getDayName(index), stat.focusTime.toFloat())
                    },
                    levelProgress = levelProgress,
                    currentLevel = level,
                    currentExperience = experience,
                    nextLevelExperience = nextLevelExp
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load stats")
                // Fallback на мок-данные при ошибке
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
                    nextLevelExperience = mockStats.nextLevelExperience,
                    error = "Не удалось загрузить статистику: ${e.message}"
                )
            }
        }
    }

    private fun getDayName(index: Int): String {
        val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - 2 + index) % 7
        return days[if (dayOfWeek < 0) dayOfWeek + 7 else dayOfWeek]
    }

    private fun createActivityDataFromStats(dailyStats: List<com.irlquest.app.data.network.dto.DailyStatsDto>): List<ActivityDay> {
        return dailyStats.takeLast(30).map { stat ->
            val intensity = when {
                stat.tasksCompleted >= 10 -> 4
                stat.tasksCompleted >= 7 -> 3
                stat.tasksCompleted >= 4 -> 2
                stat.tasksCompleted >= 1 -> 1
                else -> 0
            }
            ActivityDay(stat.date, intensity)
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