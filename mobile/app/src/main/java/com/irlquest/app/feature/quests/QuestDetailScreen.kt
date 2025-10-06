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
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Surface
        }

        val quest = uiState.quest
        if (quest == null) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(text = uiState.error ?: "Квест не найден")
            }
            return@Surface
        }

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

            Text(text = "Задачи", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Список задач в квесте
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(quest.tasks) { task ->
                    TaskRow(task = task, onToggle = { viewModel.toggleTaskCompletion(task.id) }, onClick = { onTaskClick(task.id) })
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
                if (task.description.isNotBlank()) Text(text = task.description, style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "${task.experienceReward} XP")
        }
    }
}
