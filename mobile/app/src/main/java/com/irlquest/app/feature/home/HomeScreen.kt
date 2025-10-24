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
import com.irlquest.app.ui.viewmodel.AuthViewModel
import com.irlquest.app.ui.theme.*
import com.irlquest.app.ui.utils.toQuestTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToQuest: (Int) -> Unit = {},
    onNavigateToTask: (Int) -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
    questsViewModel: QuestsViewModel = viewModel(),
    tasksViewModel: TasksViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val questsState by questsViewModel.uiState.collectAsState()
    val tasksState by tasksViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        // authViewModel.fetchMe() // Temporarily disabled - endpoint returns 404
        questsViewModel.loadQuests()
        tasksViewModel.loadTasks()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            
            // 🏰 Заголовок таверны
            Text(
                text = "⚔️ Таверна Героя ⚔️",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // 👤 Карточка персонажа
            HeroCard(currentUser)

            Spacer(modifier = Modifier.height(16.dp))

            // 📜 Доска квестов
            Text(
                text = "📜 Доска Квестов",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            QuestsSection(
                questsState = questsState,
                onNavigateToQuest = onNavigateToQuest
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Список задач
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✅ Задания Дня",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                TextButton(onClick = { tasksViewModel.showCreateDialog() }) {
                    Text(text = "+ Добавить", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TasksSection(
                tasksState = tasksState,
                onNavigateToTask = onNavigateToTask,
                onToggleTask = { taskId -> tasksViewModel.toggleTask(taskId) },
                onDeleteTask = { taskId -> tasksViewModel.deleteTask(taskId) }
            )
        }
    }
}

@Composable
private fun QuestsSection(
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
                        text = "🗺️ Нет активных квестов\nНажмите + чтобы создать новое приключение!",
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
private fun TasksSection(
    tasksState: com.irlquest.app.feature.tasks.TasksUiState,
    onNavigateToTask: (Int) -> Unit,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
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
                        text = "📝 Список задач пуст\nОтличная работа!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasksState.tasks) { t ->
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
private fun HeroCard(user: UserDto?) {
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
                        text = "💰",
                        fontSize = 20.sp
                    )
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
private fun getCharacterClassName(classKey: String): String {
    return when (classKey.lowercase()) {
        "warrior" -> "Воин"
        "mage" -> "Маг"
        "rogue" -> "Плут"
        "cleric" -> "Жрец"
        else -> "Искатель приключений"
    }
}

private fun calculateXPProgress(xp: Int): Float {
    // Упрощенная формула: каждый уровень требует level * 100 XP
    val currentLevelXP = (xp % 100).toFloat()
    return (currentLevelXP / 100f).coerceIn(0f, 1f)
}

private fun getXPForNextLevel(level: Int): Int {
    return level * 100
}

private fun getRankName(level: Int): String {
    return when {
        level < 5 -> "Новичок"
        level < 10 -> "Искатель"
        level < 20 -> "Герой"
        level < 30 -> "Чемпион"
        else -> "Легенда"
    }
}

@Composable
private fun CompactQuestCard(quest: QuestUi, onClick: () -> Unit) {
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
private fun TaskRow(t: TaskUi, onClick: () -> Unit, onToggle: () -> Unit, onDelete: () -> Unit) {
    // Определяем цвет и иконку по приоритету
    val priorityColor = when (t.priority) {
        TaskPriority.CRITICAL -> QuestLegendary
        TaskPriority.HIGH -> Warning
        TaskPriority.MEDIUM -> Info
        TaskPriority.LOW -> Neutral
    }
    
    val priorityIcon = when (t.priority) {
        TaskPriority.CRITICAL -> "⚡"
        TaskPriority.HIGH -> "🔥"
        TaskPriority.MEDIUM -> "📌"
        TaskPriority.LOW -> "🔵"
    }

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
                        text = t.title,
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
                
                if (t.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = t.description, 
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurface.copy(alpha = 0.7f)
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

