@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.irlquest.app.feature.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.ui.theme.*

@Composable
fun QuestsScreen(
    viewModel: QuestsViewModel = viewModel(),
    onNavigateToQuestDetail: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showMLGeneratorDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadQuests()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Заголовок
            Text(
                text = "📜 Доска Квестов",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = com.irlquest.app.ui.theme.TavernWood
            )
            
            // Фильтры
            QuestFilters(
                selectedFilter = uiState.selectedFilter,
                onFilterChanged = viewModel::setFilter
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredQuests) { quest ->
                        QuestCard(
                            quest = quest,
                            onClick = { onNavigateToQuestDetail(quest.id) }
                        )
                    }
                }
            }
        }
        
        // FABs внизу справа
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ML генератор квестов
            FloatingActionButton(
                onClick = { showMLGeneratorDialog = true },
                containerColor = MagicPurple,
                contentColor = com.irlquest.app.ui.theme.OnPrimary
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Сгенерировать квест с ИИ")
            }

            // Создание вручную
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = com.irlquest.app.ui.theme.Primary,
                contentColor = com.irlquest.app.ui.theme.OnPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Создать квест")
            }
        }
        
        if (showCreateDialog) {
            CreateQuestDialog(
                onDismiss = { showCreateDialog = false },
                onCreateQuest = { title, description, difficulty ->
                    viewModel.createQuest(title, description, difficulty)
                    showCreateDialog = false
                }
            )
        }

        if (showMLGeneratorDialog) {
            QuestGeneratorDialog(
                onDismiss = { showMLGeneratorDialog = false },
                onAccept = { generated ->
                    viewModel.createQuestFromML(generated)
                    showMLGeneratorDialog = false
                }
            )
        }
    }
}

@Composable
fun QuestFilters(
    selectedFilter: QuestFilter,
    onFilterChanged: (QuestFilter) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        QuestFilter.values().forEach { filter ->
            val isSelected = selectedFilter == filter
            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onFilterChanged(filter) },
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                tonalElevation = if (isSelected) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = filter.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = filter.displayName,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestCard(
    quest: QuestUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок и статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (quest.description.isNotEmpty()) {
                        Text(
                            text = quest.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                QuestStatusBadge(status = quest.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Прогресс
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = quest.completionPercentage / 100f,
                    modifier = Modifier.weight(1f),
                    color = getProgressColor(quest.completionPercentage)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${quest.completionPercentage}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Метаданные
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Сложность
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(quest.difficulty) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MysticBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        repeat(5 - quest.difficulty) {
                            Icon(
                                Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    // Количество задач
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${quest.completedTasks}/${quest.totalTasks}",
                            fontSize = 12.sp
                        )
                    }
                    
                    // Опыт за квест
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Orange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${quest.experienceReward} XP",
                            fontSize = 12.sp,
                            color = Orange
                        )
                    }
                }
                
                // Приоритет
                PriorityIndicator(priority = quest.priority)
            }
            
            // Дедлайн
            if (quest.deadline != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (quest.isOverdue) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = quest.deadline,
                        fontSize = 12.sp,
                        color = if (quest.isOverdue) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (quest.isOverdue) {
                        Text(
                            text = "ПРОСРОЧЕН",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier
                                .background(
                                    Color.Red.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuestStatusBadge(status: QuestStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        QuestStatus.ACTIVE -> Triple(Success.copy(alpha = 0.1f), Success, "Активен")
        QuestStatus.COMPLETED -> Triple(MysticBlue.copy(alpha = 0.1f), MysticBlue, "Завершен")
        QuestStatus.PAUSED -> Triple(Warning.copy(alpha = 0.1f), Warning, "Приостановлен")
        QuestStatus.ARCHIVED -> Triple(Neutral.copy(alpha = 0.1f), Neutral, "Архив")
    }
    
    Box(
        modifier = Modifier
            .background(backgroundColor, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun PriorityIndicator(priority: QuestPriority) {
    val color = when (priority) {
        QuestPriority.LOW -> Neutral
        QuestPriority.MEDIUM -> Warning
        QuestPriority.HIGH -> Error
        QuestPriority.CRITICAL -> QuestLegendary
    }
    
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuestDialog(
    onDismiss: () -> Unit,
    onCreateQuest: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(3) }
    
    // Фэнтези-стиль диалога
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.irlquest.app.ui.theme.Surface,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Иконка квеста
                Text(
                    "Новый Квест",
                    fontWeight = FontWeight.Bold,
                    color = com.irlquest.app.ui.theme.TavernWood
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название приключения") },
                    placeholder = { Text("Например: Изучить Kotlin") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.irlquest.app.ui.theme.Primary,
                        focusedLabelColor = com.irlquest.app.ui.theme.Primary,
                        cursorColor = com.irlquest.app.ui.theme.Primary
                    )
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Легенда квеста") },
                    placeholder = { Text("Опциально: расскажите подробнее") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.irlquest.app.ui.theme.Primary,
                        focusedLabelColor = com.irlquest.app.ui.theme.Primary,
                        cursorColor = com.irlquest.app.ui.theme.Primary
                    )
                )
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Сложность:",
                            fontWeight = FontWeight.Medium,
                            color = com.irlquest.app.ui.theme.TavernWood
                        )
                        Text(
                            when (difficulty) {
                                1 -> "Легкий"
                                2 -> "Средний"
                                3, 4 -> "Сложный"
                                else -> "Легендарный"
                            },
                            fontWeight = FontWeight.Bold,
                            color = when (difficulty) {
                                1 -> com.irlquest.app.ui.theme.QuestBronze
                                2 -> com.irlquest.app.ui.theme.QuestSilver
                                3, 4 -> com.irlquest.app.ui.theme.QuestGold
                                else -> com.irlquest.app.ui.theme.QuestLegendary
                            }
                        )
                    }
                    Slider(
                        value = difficulty.toFloat(),
                        onValueChange = { difficulty = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = com.irlquest.app.ui.theme.Primary,
                            activeTrackColor = com.irlquest.app.ui.theme.Primary
                        )
                    )
                    
                    // Подсказка по наградам
                    Text(
                        text = "Награда: +${difficulty * 20} золота, +${difficulty * 50} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.irlquest.app.ui.theme.OnSurface.copy(alpha = 0.6f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                        onCreateQuest(title.trim(), description.trim(), difficulty)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.irlquest.app.ui.theme.Primary,
                    contentColor = com.irlquest.app.ui.theme.OnPrimary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("🎉 Принять квест")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = com.irlquest.app.ui.theme.TavernWood)
            }
        }
    )
}

private fun getProgressColor(percentage: Int): Color {
    return when {
        percentage < 30 -> Error
        percentage < 70 -> Warning
        else -> Success
    }
}