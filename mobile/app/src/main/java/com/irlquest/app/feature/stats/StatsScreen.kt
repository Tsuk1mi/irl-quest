package com.irlquest.app.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "📊 Статистика Героя",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = com.irlquest.app.ui.theme.TavernWood
            )
        }

        // Прогресс уровня
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Ваш уровень",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = uiState.levelProgress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Уровень ${uiState.currentLevel}")
                        Text(text = "${uiState.currentExperience}/${uiState.nextLevelExperience} XP")
                    }
                }
            }
        }

        // Статистика фокуса
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Фокус",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    StatsChart(
                        data = uiState.focusTimeStats,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
        }

        // Профиль пользователя
        item {
            UserProfileCard(userProfile = uiState.userProfile)
        }

        // Статистика за сегодня
        item {
            TodayStatsCard(todayStats = uiState.todayStats)
        }

        // Недельная статистика
        item {
            WeeklyStatsCard(weeklyData = uiState.weeklyData)
        }

        // Достижения
        item {
            AchievementsSection(achievements = uiState.achievements)
        }

        // Активность
        item {
            ActivityHeatmapCard(activityData = uiState.activityData)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileCard(userProfile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userProfile.username.take(1).uppercase(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userProfile.username,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Уровень ${userProfile.level}",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Прогресс до следующего уровня
                LinearProgressIndicator(
                    progress = userProfile.experienceProgress,
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = Color.White.copy(alpha = 0.3f),
                    color = Color.White
                )
                
                Text(
                    text = "${userProfile.experience} / ${userProfile.nextLevelExperience} опыта",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayStatsCard(todayStats: TodayStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Сегодня",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    value = todayStats.completedTasks.toString(),
                    label = "Задач",
                    color = Color.Green
                )
                StatItem(
                    icon = Icons.Default.Timer,
                    value = todayStats.focusMinutes.toString(),
                    label = "Минут фокуса",
                    color = Color.Blue
                )
                StatItem(
                    icon = Icons.Default.Star,
                    value = "${todayStats.experienceGained}",
                    label = "Опыта",
                    color = MysticBlue
                )
                StatItem(
                    icon = Icons.Default.Favorite,
                    value = "${todayStats.productivityScore}%",
                    label = "Продуктивность",
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyStatsCard(weeklyData: List<DayData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Активность за неделю",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Линия графика активности
            val chartColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                drawWeeklyChart(weeklyData, size, chartColor)
            }
            
            // Легенда дней недели
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyData.forEach { day ->
                    Text(
                        text = day.dayName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsSection(achievements: List<Achievement>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Достижения",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievements) { achievement ->
                    AchievementBadge(achievement = achievement)
                }
            }
        }
    }
}

@Composable
fun AchievementBadge(achievement: Achievement) {
    Card(
        modifier = Modifier.size(80.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = if (achievement.isUnlocked) achievement.color else Color.Gray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = achievement.emoji,
                fontSize = 24.sp
            )
            Text(
                text = achievement.name,
                fontSize = 10.sp,
                color = if (achievement.isUnlocked) Color.White else Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHeatmapCard(activityData: List<ActivityDay>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Карта активности (30 дней)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Простая сетка для карты активности
            Column {
                repeat(5) { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(7) { day ->
                            val index = week * 7 + day
                            if (index < activityData.size) {
                                val activity = activityData[index]
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            color = getActivityColor(activity.intensity),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                            } else {
                                Spacer(modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    if (week < 4) Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun StatisticsItem(/* параметры */) {
    // ...существующий код...
}

@Composable
fun renderStatItem(/* параметры */) {
    // ...существующий код...
}

@Composable
fun renderChart(/* параметры */) {
    // ...существующий код...
}

private fun DrawScope.drawWeeklyChart(weeklyData: List<DayData>, size: androidx.compose.ui.geometry.Size, color: androidx.compose.ui.graphics.Color) {
    if (weeklyData.isEmpty()) return
    
    val maxValue = weeklyData.maxOfOrNull { it.value } ?: 0f
    if (maxValue <= 0) return
    
    val width = size.width
    val height = size.height
    val stepWidth = width / weeklyData.size
    
    val path = Path()
    weeklyData.forEachIndexed { index, dayData ->
        val x = index * stepWidth + stepWidth / 2
        val y = height - (dayData.value / maxValue * height)
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
        
        // Рисуем точку
        drawCircle(
            color = color,
            radius = 4.dp.toPx(),
            center = Offset(x, y)
        )
    }
    
    // Рисуем линию
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
    )
}

private fun getActivityColor(intensity: Int): Color {
    return when (intensity) {
        0 -> Color.Gray.copy(alpha = 0.1f)
        1 -> Color.Green.copy(alpha = 0.3f)
        2 -> Color.Green.copy(alpha = 0.6f)
        3 -> Color.Green.copy(alpha = 0.8f)
        else -> Color.Green
    }
}