package com.irlquest.app.feature.tasks

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.feature.auth.AuthViewModel
import com.irlquest.app.ui.theme.*
import com.irlquest.app.ui.components.RewardDialog
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onNavigateToTaskDetail: (Int) -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
    viewModel: TasksViewModel = remember { TasksViewModel(authViewModel) }
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "✅ Мои Задания",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TavernWood
                )
                Text(
                    text = "🗓️ Сегодня: ${uiState.todaySummary.completed}/${uiState.todaySummary.total} выполнено",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.todaySummary.completed == uiState.todaySummary.total && uiState.todaySummary.total > 0)
                        Success
                    else
                        TavernWood.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Task list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.tasks) { task ->
                        TaskItem(
                            task = task,
                            onTaskToggle = { viewModel.toggleTask(task.id) },
                            onTaskDelete = { viewModel.deleteTask(task.id) },
                            onClick = { onNavigateToTaskDetail(task.id) }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { viewModel.showCreateDialog() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Primary,
            contentColor = OnPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Создать задачу"
            )
        }
    }

    // Create task dialog
    if (uiState.showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onCreateTask = { title, description, priority, difficulty, deadline, aiPick ->
                viewModel.createTask(title, description, priority, difficulty, deadline, aiPick)
            }
        )
    }
    
    // Reward dialog при завершении задачи
    if (uiState.showRewardDialog && uiState.lastCompletedTask != null) {
        val task = uiState.lastCompletedTask!!
        RewardDialog(
            questTitle = task.title,
            xpGained = task.experienceReward,
            goldGained = task.difficulty * 10,
            levelUp = uiState.leveledUp,
            newLevel = uiState.newLevel,
            onDismiss = { viewModel.dismissRewardDialog() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: TaskUi,
    onTaskToggle: () -> Unit,
    onTaskDelete: () -> Unit,
    onClick: () -> Unit = {}
) {
    val priorityColor = when (task.priority) {
        TaskPriority.CRITICAL -> QuestLegendary
        TaskPriority.HIGH -> Warning
        TaskPriority.MEDIUM -> Info
        TaskPriority.LOW -> Neutral
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (!task.completed) {
                    Modifier.border(
                        width = 2.dp,
                        color = priorityColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) 
                Success.copy(alpha = 0.05f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Чекбокс с анимацией
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = { onTaskToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Success,
                        uncheckedColor = priorityColor
                    )
                )
                
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = if (task.completed) 
                            MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                color = OnSurface.copy(alpha = 0.6f)
                            )
                        else 
                            MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Информация о задаче
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Приоритет
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (task.priority) {
                                        TaskPriority.CRITICAL -> "⚡"
                                        TaskPriority.HIGH -> "🔥"
                                        TaskPriority.MEDIUM -> "📌"
                                        TaskPriority.LOW -> "🔵"
                                    },
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = task.priority.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = priorityColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Дедлайн
                            task.deadline?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "⏰", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (task.isOverdue) Error else Warning,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        // Награды
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⭐ ${task.experienceReward}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MysticBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "💰 ${task.difficulty * 10}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                IconButton(onClick = onTaskDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить задачу",
                        tint = Error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreateTask: (String, String, TaskPriority, Int, String?, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var difficulty by remember { mutableStateOf(3) }
    var aiPick by remember { mutableStateOf(false) }
    var deadline by remember { mutableStateOf("") }
    
    // Расчёт наград
    val xpReward = difficulty * 10 + when (priority) {
        TaskPriority.CRITICAL -> 20
        TaskPriority.HIGH -> 10
        TaskPriority.MEDIUM -> 5
        TaskPriority.LOW -> 0
    }
    val goldReward = difficulty * 10

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨", fontSize = 32.sp, modifier = Modifier.padding(end = 8.dp))
                Text(
                    "Новое Задание",
                    fontWeight = FontWeight.Bold,
                    color = TavernWood
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
                    label = { Text("⚔️ Название подвига") },
                    placeholder = { Text("Например: Изучить Kotlin") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary,
                        cursorColor = Primary
                    )
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("📖 Описание (опционально)") },
                    placeholder = { Text("Что нужно сделать?") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary,
                        cursorColor = Primary
                    )
                )
                
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("⏰ Дедлайн (опционально)") },
                    placeholder = { Text("дд.мм.гггг") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Warning,
                        focusedLabelColor = Warning,
                        cursorColor = Warning
                    )
                )

                // Priority selector
                Column {
                    Text(
                        "🎯 Приоритет:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TavernWood
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskPriority.values().forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            when (p) {
                                                TaskPriority.CRITICAL -> "⚡"
                                                TaskPriority.HIGH -> "🔥"
                                                TaskPriority.MEDIUM -> "📌"
                                                TaskPriority.LOW -> "🔵"
                                            },
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(p.displayName)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (p) {
                                        TaskPriority.CRITICAL -> QuestLegendary.copy(alpha = 0.2f)
                                        TaskPriority.HIGH -> Warning.copy(alpha = 0.2f)
                                        TaskPriority.MEDIUM -> Info.copy(alpha = 0.2f)
                                        TaskPriority.LOW -> Neutral.copy(alpha = 0.2f)
                                    }
                                )
                            )
                        }
                    }
                }
                
                // Сложность
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "🎲 Сложность:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TavernWood
                        )
                        Text(
                            when (difficulty) {
                                1 -> "🥉 Легко"
                                2 -> "🥈 Средне"
                                3, 4 -> "🥇 Сложно"
                                else -> "⚡ Героически"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = difficulty.toFloat(),
                        onValueChange = { difficulty = it.toInt(); aiPick = false },
                        valueRange = 1f..5f,
                        steps = 3,
                        enabled = !aiPick,
                        colors = SliderDefaults.colors(
                            thumbColor = Primary,
                            activeTrackColor = Primary
                        )
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = aiPick, onCheckedChange = { aiPick = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "🤖 Пусть ИИ выберет сложность",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Предпросмотр наград
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Primary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "🎁 Награды за выполнение:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "⭐ +$xpReward",
                                fontWeight = FontWeight.Bold,
                                color = MysticBlue
                            )
                            Text(
                                "💰 +$goldReward",
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
                
                // Подсказка про ИИ
                if (aiPick) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MagicPurple.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🤖", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ИИ автоматически определит сложность на основе описания задачи",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MagicPurple
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreateTask(title.trim(), description.trim(), priority, difficulty, if (deadline.isBlank()) null else deadline, aiPick)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("⚔️ Принять задание")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TavernWood)
            }
        }
    )
}
