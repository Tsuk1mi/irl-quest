package com.irlquest.app.feature.quests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.data.network.dto.TaskDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestDetailScreen(
    questId: Int,
    onTaskClick: (Int) -> Unit = {},
    viewModel: QuestDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(questId) {
        viewModel.loadQuest(questId)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.quest == null -> {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(text = uiState.error ?: "Квест не найден")
                }
            }
            else -> {
                val quest = uiState.quest!!
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = quest.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            quest.description?.let { desc ->
                if (desc.isNotBlank()) Text(text = desc, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Сложность: ${quest.difficulty}")
                    Text(text = "Приоритет: ${quest.priority}")
                    Text(text = "Статус: ${quest.status}")
                    Text(text = "Опыт: ${quest.experienceReward}")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Задачи", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { viewModel.showAddTaskDialog = true }) {
                    Text("+ Добавить задачу")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Список задач в квесте
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(quest.tasks) { task ->
                    TaskRow(task = task, onToggle = { viewModel.toggleTaskCompletion(task.id) }, onClick = { onTaskClick(task.id) })
                }
            }

            // Кнопка завершения квеста
            if (quest.status != QuestStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.requestQuestVerification(
                            questId = quest.id,
                            questTitle = quest.title,
                            questDescription = quest.description,
                            userLevel = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.irlquest.app.ui.theme.Success
                    )
                ) {
                    Text("✅ Завершить квест")
                }
            }

            // Диалог добавления задачи
            if (viewModel.showAddTaskDialog) {
                AddTaskToQuestDialog(
                    questId = quest.id,
                    onDismiss = { viewModel.showAddTaskDialog = false },
                    onAddTask = { title, description ->
                        viewModel.addTaskToQuest(quest.id, title, description)
                        viewModel.showAddTaskDialog = false
                    }
                )
            }

            // Диалог верификации
            if (uiState.showVerificationDialog && uiState.verification != null) {
                QuestVerificationDialog(
                    questTitle = quest.title,
                    verification = uiState.verification!!,
                    onQuizSubmit = { answers ->
                        viewModel.submitQuizAnswers(quest.id, answers)
                    },
                    onPhotoSubmit = { imageBase64 ->
                        viewModel.submitPhotoVerification(quest.id, imageBase64)
                    },
                    onDismiss = { viewModel.dismissVerificationDialog() }
                )
            }

            // Результат верификации
            uiState.verificationResult?.let { result ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearVerificationResult() },
                    title = { Text("Результат проверки") },
                    text = { Text(result) },
                    confirmButton = {
                        Button(onClick = { viewModel.clearVerificationResult() }) {
                            Text("OK")
                        }
                    }
                )
            }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(task: TaskDto, onToggle: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
                if (!task.description.isNullOrBlank()) Text(text = task.description, style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "${task.experienceReward} XP")
        }
    }
}

@Composable
fun AddTaskToQuestDialog(
    questId: Int,
    onDismiss: () -> Unit,
    onAddTask: (String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить задачу в квест") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название задачи") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAddTask(title, description.takeIf { it.isNotBlank() })
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
