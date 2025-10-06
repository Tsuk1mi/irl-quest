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
import com.irlquest.app.ui.theme.Orange

@Composable
fun QuestsScreen(
    viewModel: QuestsViewModel = viewModel(),
    onNavigateToQuestDetail: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadQuests()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Заголовок и фильтры
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Квесты",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Создать квест",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
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
        
        if (showCreateDialog) {
            CreateQuestDialog(
                onDismiss = { showCreateDialog = false },
                onCreateQuest = { title, description, difficulty ->
                    viewModel.createQuest(title, description, difficulty)
                    showCreateDialog = false
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
                                tint = Orange,
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
        QuestStatus.ACTIVE -> Triple(Color.Green.copy(alpha = 0.1f), Color.Green, "Активен")
        QuestStatus.COMPLETED -> Triple(Color.Blue.copy(alpha = 0.1f), Color.Blue, "Завершен")
        QuestStatus.PAUSED -> Triple(Orange.copy(alpha = 0.1f), Orange, "Приостановлен")
        QuestStatus.ARCHIVED -> Triple(Color.Gray.copy(alpha = 0.1f), Color.Gray, "Архив")
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
        QuestPriority.LOW -> Color.Green
        QuestPriority.MEDIUM -> Orange
        QuestPriority.HIGH -> Color.Red
        QuestPriority.CRITICAL -> Color.Red
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
    var difficulty by remember { mutableStateOf(1) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать квест") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название квеста") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Column {
                    Text("Сложность: $difficulty")
                    Slider(
                        value = difficulty.toFloat(),
                        onValueChange = { difficulty = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                        onCreateQuest(title, description, difficulty)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun getProgressColor(percentage: Int): Color {
    return when {
        percentage < 30 -> Color.Red
        percentage < 70 -> Orange
        else -> Color.Green
    }
}