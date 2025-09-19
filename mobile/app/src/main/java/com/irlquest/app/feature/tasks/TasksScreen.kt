package com.irlquest.app.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TasksScreen(
    viewModel: TasksViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadTasks()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Заголовок и действия
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Мои задачи",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Сегодня: ${uiState.todaySummary.completed}/${uiState.todaySummary.total}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { /* TODO: Открыть настройки */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Создать задачу",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Фильтры
            TaskFilters(
                selectedFilter = uiState.selectedFilter,
                onFilterChanged = viewModel::setFilter
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Прогресс дня
            if (uiState.todaySummary.total > 0) {
                DayProgressCard(
                    completed = uiState.todaySummary.completed,
                    total = uiState.todaySummary.total,
                    experience = uiState.todaySummary.experienceGained
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Список задач
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredTasks) { task ->
                        TaskItem(
                            task = task,
                            onTaskToggle = { viewModel.toggleTask(task.id) },
                            onTaskEdit = { /* TODO: Редактировать задачу */ },
                            onTaskDelete = { viewModel.deleteTask(task.id) }
                        )
                    }
                    
                    if (uiState.filteredTasks.isEmpty()) {
                        item {
                            EmptyState(filter = uiState.selectedFilter)
                        }
                    }
                }
            }
        }
        
        // Диалог создания задачи
        if (showCreateDialog) {
            CreateTaskDialog(
                onDismiss = { showCreateDialog = false },
                onCreateTask = { title, description, priority ->
                    viewModel.createTask(title, description, priority)
                    showCreateDialog = false
                }
            )
        }
        
        // Сообщение об ошибке
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // TODO: Показать Snackbar с ошибкой
            }
        }
    }
}

@Composable
fun TaskFilters(
    selectedFilter: TaskFilter,
    onFilterChanged: (TaskFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TaskFilter.values()) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChanged(filter) },
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = filter.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(filter.displayName)
                    }
                }
            )
        }
    }
}

@Composable
fun DayProgressCard(
    completed: Int,
    total: Int,
    experience: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Прогресс дня",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = if (total > 0) completed.toFloat() / total else 0f,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colors.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "$completed из $total задач",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                horizontalAlignment = Alignment.CenterAlignment
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.Orange,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "+$experience XP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Orange
                )
            }
        }
    }
}

@Composable
fun TaskItem(
    task: TaskUi,
    onTaskToggle: () -> Unit,
    onTaskEdit: () -> Unit,
    onTaskDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onTaskToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colors.primary
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Содержимое задачи
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = if (task.completed) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.completed) 
                        MaterialTheme.colors.onSurface.copy(alpha = 0.6f) 
                    else MaterialTheme.colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                // Метаданные
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Приоритет
                    TaskPriorityIndicator(priority = task.priority)
                    
                    // Дедлайн
                    if (task.deadline != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (task.isOverdue) Color.Red 
                                      else MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = task.deadline,
                                fontSize = 10.sp,
                                color = if (task.isOverdue) Color.Red 
                                       else MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    
                    // Награда за опыт
                    if (task.experienceReward > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.Orange
                            )
                            Text(
                                text = "${task.experienceReward}",
                                fontSize = 10.sp,
                                color = Color.Orange
                            )
                        }
                    }
                }
            }
            
            // Меню действий
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(onClick = {
                        showMenu = false
                        onTaskEdit()
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Редактировать")
                    }
                    DropdownMenuItem(onClick = {
                        showMenu = false
                        onTaskDelete()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Удалить", color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskPriorityIndicator(priority: TaskPriority) {
    val (color, text) = when (priority) {
        TaskPriority.LOW -> Color.Green to "Низкий"
        TaskPriority.MEDIUM -> Color.Orange to "Средний"
        TaskPriority.HIGH -> Color.Red to "Высокий"
        TaskPriority.CRITICAL -> Color.Red to "Критический"
    }
    
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreateTask: (String, String, TaskPriority) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая задача") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название задачи") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Column {
                    Text("Приоритет:")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskPriority.values().forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                content = { Text(p.displayName) }
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
                        onCreateTask(title, description, priority)
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

@Composable
fun EmptyState(filter: TaskFilter) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Assignment,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val message = when (filter) {
            TaskFilter.ALL -> "У вас пока нет задач"
            TaskFilter.ACTIVE -> "Нет активных задач"
            TaskFilter.COMPLETED -> "Нет завершенных задач"
            TaskFilter.HIGH_PRIORITY -> "Нет важных задач"
            TaskFilter.OVERDUE -> "Нет просроченных задач"
        }
        
        Text(
            text = message,
            fontSize = 16.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        
        if (filter == TaskFilter.ALL) {
            Text(
                text = "Создайте свою первую задачу!",
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}