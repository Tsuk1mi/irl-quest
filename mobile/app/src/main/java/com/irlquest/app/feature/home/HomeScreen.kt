package com.irlquest.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.data.network.dto.UserDto
import com.irlquest.app.feature.quests.QuestUi
import com.irlquest.app.feature.quests.QuestsViewModel
import com.irlquest.app.feature.tasks.TaskUi
import com.irlquest.app.feature.tasks.TasksViewModel
import com.irlquest.app.feature.tasks.TaskPriority
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.feature.auth.AuthViewModel
import com.irlquest.app.ui.theme.*
import com.irlquest.app.ui.utils.toQuestTitle
import com.irlquest.app.ui.components.RewardDialog
import androidx.compose.runtime.remember
import com.irlquest.app.feature.home.MultiplayerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToQuest: (Int) -> Unit = {},
    onNavigateToTask: (Int) -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToGuilds: () -> Unit = {},
    onNavigateToCoop: () -> Unit = {},
    onNavigateToAuction: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
    questsViewModel: QuestsViewModel = viewModel(),
    tasksViewModel: TasksViewModel = remember { TasksViewModel(authViewModel) }
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val questsState by questsViewModel.uiState.collectAsState()
    val tasksState by tasksViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        // authViewModel.fetchMe() // Temporarily disabled - endpoint returns 404
        questsViewModel.loadQuests()
        tasksViewModel.loadTasks()
        
        // Генерируем ежедневные задачи на основе активности (отключено для снижения нагрузки при старте)
        // tasksViewModel.generateDailyTasksFromActivity()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.background
                )
            )
        )) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Primary.copy(alpha = 0.2f),
                                    PrimaryDark.copy(alpha = 0.1f),
                                    Primary.copy(alpha = 0.2f)
                                )
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏰 Таверна Героя",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TavernWood,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Primary.copy(alpha = 0.3f),
                                blurRadius = 8f
                            )
                        )
                    )
                }
            }

            item {
                // 👤 Карточка персонажа
                HeroCard(currentUser)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📜 Доска Квестов",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TavernWood
                        )
                    }
                }
            }

            item {
                QuestsSection(
                    questsState = questsState,
                    onNavigateToQuest = onNavigateToQuest
                )
            }

            item {
                // ✅ Список задач
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✅ Задания Дня",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TavernWood
                        )
                    }
                    Button(
                        onClick = { tasksViewModel.showCreateDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Secondary
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "+ Добавить",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                TasksSection(
                    tasksState = tasksState,
                    onNavigateToTask = onNavigateToTask,
                    onToggleTask = { taskId -> tasksViewModel.toggleTask(taskId) },
                    onDeleteTask = { taskId -> tasksViewModel.deleteTask(taskId) }
                )
            }

            item {
                // Фокус-сессии
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🎯 Фокус",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TavernWood
                    )
                }
            }

            item {
                MultiplayerCard(
                    title = "🧘 Фокус-сессии",
                    description = "Сконцентрируйся на задачах",
                    onClick = onNavigateToFocus,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                // Мультиплеер секция
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⚔️ Мультиплеер и Торговля",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TavernWood
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MultiplayerCard(
                        title = "🛡️ Гильдии",
                        description = "Присоединяйтесь к гильдиям и сражайтесь вместе",
                        onClick = onNavigateToGuilds,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MultiplayerCard(
                        title = "⚔️ Кооп-миссии",
                        description = "Выполняйте эпические квесты в команде",
                        onClick = onNavigateToCoop,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MultiplayerCard(
                        title = "📋 Все задания",
                        description = "Полный список ваших заданий",
                        onClick = onNavigateToTasks,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MultiplayerCard(
                        title = "🎲 Аукцион героев",
                        description = "Покупайте и продавайте артефакты",
                        onClick = onNavigateToAuction,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Показываем награды при завершении задачи
        if (tasksState.showRewardDialog && tasksState.lastCompletedTask != null) {
            val task = tasksState.lastCompletedTask!!
            RewardDialog(
                questTitle = task.title,
                xpGained = task.experienceReward,
                goldGained = task.difficulty * 10,
                levelUp = tasksState.leveledUp,
                newLevel = tasksState.newLevel,
                loot = tasksState.recentLoot,
                onDismiss = { tasksViewModel.dismissRewardDialog() }
            )
        }
    }
}

@Composable
fun QuestsSection(
    questsState: com.irlquest.app.feature.quests.QuestsUiState,
    onNavigateToQuest: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().height(160.dp)
    ) {
        when {
            questsState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            questsState.filteredQuests.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Нет активных квестов\nНажмите + чтобы создать новое приключение!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            else -> {
                LazyRow(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(questsState.filteredQuests.take(6)) { q ->
                        CompactQuestCard(q, onClick = { onNavigateToQuest(q.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun TasksSection(
    tasksState: com.irlquest.app.feature.tasks.TasksUiState,
    onNavigateToTask: (Int) -> Unit,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp), // Фиксированная высота для избежания бесконечных constraint
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        when {
            tasksState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            tasksState.tasks.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Список задач пуст\nОтличная работа!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            else -> {
                // Показываем только первые 3 задачи для компактности
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tasksState.tasks.take(3).forEach { t ->
                        TaskRow(
                            t,
                            onClick = { onNavigateToTask(t.id) },
                            onToggle = { onToggleTask(t.id) },
                            onDelete = { onDeleteTask(t.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroCard(user: UserDto?) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎭 ${user?.username ?: "Гость"}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Уровень ${user?.level ?: 1} • ${getCharacterClassName(user?.characterClass ?: "warrior")}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Полоса опыта
                LinearProgressIndicator(
                    progress = calculateXPProgress(user?.experience ?: 0).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Text(
                    text = "XP: ${user?.experience ?: 0} / ${getXPForNextLevel(user?.level ?: 1)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            // Статистика справа
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = " ${user?.gold ?: 0}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⭐ Ранг: ${getRankName(user?.level ?: 1)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Вспомогательные функции
fun getCharacterClassName(classKey: String): String {
    return when (classKey.lowercase()) {
        "warrior" -> "Воин"
        "mage" -> "Маг"
        "rogue" -> "Плут"
        "cleric" -> "Жрец"
        else -> "Искатель приключений"
    }
}

fun calculateXPProgress(xp: Int): Float {
    // Упрощенная формула: каждый уровень требует level * 100 XP
    val currentLevelXP = (xp % 100).toFloat()
    return (currentLevelXP / 100f).coerceIn(0f, 1f)
}

fun getXPForNextLevel(level: Int): Int {
    return level * 100
}

fun getRankName(level: Int): String {
    return when {
        level < 5 -> "Новичок"
        level < 10 -> "Искатель"
        level < 20 -> "Герой"
        level < 30 -> "Чемпион"
        else -> "Легенда"
    }
}

@Composable
fun CompactQuestCard(quest: QuestUi, onClick: () -> Unit) {
    // Используем новый компонент из дизайн-системы
    com.irlquest.app.ui.components.CompactQuestCard(
        title = quest.title,
        difficulty = quest.difficulty.coerceIn(1, 5),
        progress = quest.completedTasks,
        totalTasks = quest.totalTasks,
        onClick = onClick
    )
}

@Composable
fun TaskRow(t: TaskUi, onClick: () -> Unit, onToggle: () -> Unit, onDelete: () -> Unit) {
    // Определяем цвет и иконку по приоритету
    val priorityColor = when (t.priority) {
        TaskPriority.CRITICAL -> QuestLegendary
        TaskPriority.HIGH -> Warning
        TaskPriority.MEDIUM -> Info
        TaskPriority.LOW -> Neutral
    }
    
    // Приоритет задачи определяет цвет и стиль отображения
    val priorityIcon = when (t.priority) {
        TaskPriority.CRITICAL -> "!"
        TaskPriority.HIGH -> "H"
        TaskPriority.MEDIUM -> "M"
        TaskPriority.LOW -> "L"
    }
    
    // 🎭 Выбираем что показывать: фэнтези-версию или оригинал
    val displayTitle = if (t.showFantasyVersion && t.fantasyTitle != null) t.fantasyTitle else t.title
    val displayDesc = if (t.showFantasyVersion && t.fantasyDescription != null) {
        t.fantasyDescription.take(150) + if (t.fantasyDescription.length > 150) "..." else ""
    } else t.description

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (t.completed) Success.copy(alpha = 0.3f) else priorityColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (t.completed) 
                Success.copy(alpha = 0.05f) 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).clickable { onClick() }) {
                // Иконка и заголовок
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Иконка выполнения
                    Text(
                        text = if (t.completed) "✅" else "⭕",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    Text(
                        text = displayTitle,
                        fontWeight = FontWeight.Bold,
                        style = if (t.completed) 
                            MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                color = OnSurface.copy(alpha = 0.6f)
                            )
                        else 
                            MaterialTheme.typography.bodyLarge
                    )
                }
                
                if (displayDesc.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayDesc, 
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurface.copy(alpha = 0.7f),
                        maxLines = 3
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Информация в стиле таверны
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Дедлайн
                    t.deadline?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⏰", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = Warning
                            )
                        }
                    }
                    
                    // Приоритет со значком
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = priorityIcon,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = t.priority.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = priorityColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (!t.completed) {
                    Button(
                        onClick = onToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Success.copy(alpha = 0.2f),
                            contentColor = Success
                        ),
                        modifier = Modifier.height(36.dp)
                    ) { 
                        Text("✓ Готово", fontSize = 13.sp) 
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggle,
                        modifier = Modifier.height(36.dp)
                    ) { 
                        Text("↶ Вернуть", fontSize = 13.sp) 
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { 
                    Text(text = "🗑️ Удалить", fontSize = 12.sp) 
                }
            }
        }
    }
}

