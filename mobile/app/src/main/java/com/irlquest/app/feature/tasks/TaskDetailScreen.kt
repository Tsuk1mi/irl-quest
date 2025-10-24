package com.irlquest.app.feature.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.data.network.dto.TaskDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Int,
    onDeleted: () -> Unit = {},
    viewModel: TaskDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.task == null -> {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(text = uiState.error ?: "Задача не найдена")
                }
            }
            else -> {
                val task = uiState.task!!
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.headlineSmall)
            if (task.description.isNotBlank()) Text(text = task.description, style = MaterialTheme.typography.bodyMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Статус: ${task.status}")
                    Text(text = "Приоритет: ${task.priority}")
                    Text(text = "Сложность: ${task.difficulty}")
                    Text(text = "Оценка длительности: ${task.estimatedDuration ?: "—"}")
                    Text(text = "Опыт: ${task.experienceReward} XP")
                    Text(text = "Дедлайн: ${task.deadline ?: "—"}")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.toggleCompleted() }) {
                    Text(if (task.completed) "Отметить как не выполнено" else "Отметить как выполнено")
                }
                OutlinedButton(onClick = { viewModel.deleteTask { onDeleted() } }) {
                    Text("Удалить")
                }
            }

            uiState.error?.let { err ->
                Text(text = err, color = MaterialTheme.colorScheme.error)
            }
                }
            }
        }
    }
}

