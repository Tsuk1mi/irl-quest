package com.irlquest.app.feature.tasks

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import com.irlquest.app.feature.tasks.DiceState
import com.irlquest.app.feature.tasks.DiceType
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

    // Create task dialog with integrated dice roller
    if (uiState.showCreateDialog) {
        CreateTaskDialog(
            diceState = uiState.diceState,
            onDismiss = { viewModel.hideCreateDialog() },
            onCreateTask = { title, description, priority, deadline ->
                viewModel.createTask(title, description, priority, deadline)
            },
            onOpenDice = { viewModel.openDiceSheet() },
            onRollDice = { viewModel.rollDice() },
            onDiceTypeSelected = { viewModel.selectDiceType(it) },
            onModifierChanged = { viewModel.updateDiceModifier(it) }
        )
    }

    if (uiState.showDiceSheet) {
        DiceRollerSheet(
            diceState = uiState.diceState,
            onDismiss = { viewModel.closeDiceSheet() },
            onDiceTypeSelected = { viewModel.selectDiceType(it) },
            onModifierChanged = { viewModel.updateDiceModifier(it) },
            onRollDice = { viewModel.rollDice() }
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
            loot = uiState.recentLoot,
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
                                        TaskPriority.CRITICAL -> "!"
                                        TaskPriority.HIGH -> "H"
                                        TaskPriority.MEDIUM -> "M"
                                        TaskPriority.LOW -> "L"
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
                                    Text(text = "T", fontSize = 14.sp)
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
                                text = "XP ${task.experienceReward}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MysticBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gold 10",
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
    diceState: DiceState,
    onDismiss: () -> Unit,
    onCreateTask: (String, String, TaskPriority, String?) -> Unit,
    onOpenDice: () -> Unit,
    onRollDice: () -> Unit,
    onDiceTypeSelected: (DiceType) -> Unit,
    onModifierChanged: (Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var deadline by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 - форма, 1 - кубики
    
    // Расчёт наград
    val xpReward = 10 + when (priority) {
        TaskPriority.CRITICAL -> 20
        TaskPriority.HIGH -> 10
        TaskPriority.MEDIUM -> 5
        TaskPriority.LOW -> 0
    }
    val goldReward = 10

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { 
            Column {
                Text(
                    "✨ Новое Задание",
                    fontWeight = FontWeight.Bold,
                    color = TavernWood
                )
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Surface,
                    contentColor = Primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("📝 Форма") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("🎲 Кубики") },
                        icon = {
                            if (diceState.lastRoll != null) {
                                Text("✓", color = Success)
                            }
                        }
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Показываем разный контент в зависимости от вкладки
                when (selectedTab) {
                    0 -> {
                        // Форма создания задания
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Название подвига") },
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
                            label = { Text("Описание (опционально)") },
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
                            label = { Text("Дедлайн (опционально)") },
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
                                    "Награды за выполнение:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "XP +$xpReward",
                                        fontWeight = FontWeight.Bold,
                                        color = MysticBlue
                                    )
                                    Text(
                                        "Gold +$goldReward",
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                    
                    1 -> {
                        // Вкладка с кубиками
                        DiceTabContent(
                            diceState = diceState,
                            onDiceTypeSelected = { /* handled by parent */ },
                            onModifierChanged = { /* handled by parent */ },
                            onRollDice = onRollDice
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreateTask(title.trim(), description.trim(), priority, if (deadline.isBlank()) null else deadline)
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
                Text("Принять задание")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TavernWood)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceTabContent(
    diceState: DiceState,
    onDiceTypeSelected: (DiceType) -> Unit,
    onModifierChanged: (Int) -> Unit,
    onRollDice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎲 Бросок кубов D&D",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TavernWood
        )
        
        Text(
            text = "Киньте кости чтобы оценить стоит ли браться за задачу",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurface.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // Большая кнопка броска
        Button(
            onClick = onRollDice,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MagicPurple
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = diceState.selectedDice.icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
                Text(
                    text = "Бросить ${diceState.selectedDice.label}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Результат последнего броска
        diceState.lastRoll?.let { roll ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        roll.total >= 18 -> Success.copy(alpha = 0.2f)
                        roll.total >= 12 -> Info.copy(alpha = 0.2f)
                        else -> Warning.copy(alpha = 0.2f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = roll.total.toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            roll.total >= 18 -> Success
                            roll.total >= 12 -> Info
                            else -> Warning
                        }
                    )
                    Text(
                        text = "Результат: ${roll.rolls.joinToString(" + ")}${if (roll.modifier != 0) " + ${roll.modifier}" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface.copy(alpha = 0.8f)
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text(
                        text = when {
                            roll.total >= 18 -> "🌟 Критический успех! Эта задача вам по плечу!"
                            roll.total >= 15 -> "✨ Отличный результат! Смело беритесь за дело!"
                            roll.total >= 12 -> "👍 Хороший бросок. У вас есть шансы!"
                            roll.total >= 8 -> "🤔 Средний результат. Будьте осторожны."
                            else -> "⚠️ Низкий бросок. Возможно, стоит отложить или подготовиться лучше."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TavernWood,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } ?: run {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Бросьте кости чтобы получить совет от богов приключений",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
        
        // Выбор типа кубика
        Text(
            text = "Тип кубика:",
            style = MaterialTheme.typography.labelMedium,
            color = TavernWood
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DiceType.values().forEach { dice ->
                FilterChip(
                    selected = diceState.selectedDice == dice,
                    onClick = { onDiceTypeSelected(dice) },
                    label = { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = dice.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(dice.label, fontSize = 10.sp)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MagicPurple.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiceRollerSheet(
    diceState: DiceState,
    onDismiss: () -> Unit,
    onDiceTypeSelected: (DiceType) -> Unit,
    onModifierChanged: (Int) -> Unit,
    onRollDice: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🎲 D&D Костепад",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TavernWood
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiceType.values().forEach { type ->
                    FilterChip(
                        selected = diceState.selectedDice == type,
                        onClick = { onDiceTypeSelected(type) },
                        label = { Text(type.label) },
                        leadingIcon = {
                            Icon(
                                type.icon,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Модификатор: ${diceState.modifier}",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = diceState.modifier.toFloat(),
                    onValueChange = { onModifierChanged(it.toInt()) },
                    valueRange = -10f..10f,
                    steps = 19
                )
            }

            Button(
                onClick = onRollDice,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary
                )
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Бросить ${diceState.selectedDice.label}")
            }

            diceState.lastRoll?.let { roll ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Последний бросок",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Роллы: ${roll.rolls.joinToString()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Модификатор: ${roll.modifier}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Итог: ${roll.total}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
            }

            if (diceState.history.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "История бросков",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    diceState.history.take(5).forEach { roll ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = roll.diceType.label)
                            Text(text = roll.total.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}