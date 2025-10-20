package com.irlquest.app.feature.home

import androidx.compose.foundation.background
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
import com.irlquest.app.feature.quests.QuestUi
import com.irlquest.app.feature.quests.QuestsViewModel
import com.irlquest.app.feature.tasks.TaskUi
import com.irlquest.app.feature.tasks.TasksViewModel
import com.irlquest.app.feature.tasks.TaskPriority
import com.irlquest.app.ui.viewmodel.AuthViewModel

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
        authViewModel.fetchMe()
        questsViewModel.loadQuests()
        tasksViewModel.loadTasks()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        // Top: player level and nickname
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = "Уровень: 1", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = currentUser?.username ?: "Гость", style = MaterialTheme.typography.bodySmall)
            }
            // Quick stats
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "XP: 0", style = MaterialTheme.typography.bodySmall)
                Text(text = "Rank: Новичок", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Master window: show quests overview
        Text(text = "Мастер — квесты", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth().height(160.dp)) {
            if (questsState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val list = questsState.filteredQuests.take(6)
                if (list.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Нет активных квестов")
                    }
                } else {
                    LazyRow(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(list) { q ->
                            CompactQuestCard(q, onClick = { onNavigateToQuest(q.id) })
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // TO DO list
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "TO DO", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { tasksViewModel.showCreateDialog() }) { Text(text = "Добавить задачу") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
            if (tasksState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val todo = tasksState.tasks
                if (todo.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = "Список задач пуст") }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(todo) { t ->
                            TaskRow(t, onClick = { onNavigateToTask(t.id) }, onToggle = { tasksViewModel.toggleTask(t.id) }, onDelete = { tasksViewModel.deleteTask(t.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactQuestCard(quest: QuestUi, onClick: () -> Unit) {
    Card(modifier = Modifier.width(220.dp).fillMaxHeight().clickable { onClick() }, shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = quest.title, maxLines = 2, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = quest.completionPercentage / 100f, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "${quest.completionPercentage}% — ${quest.completedTasks}/${quest.totalTasks} задач", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TaskRow(t: TaskUi, onClick: () -> Unit, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f).clickable { onClick() }) {
                Text(text = t.title, fontWeight = FontWeight.Bold)
                if (t.description.isNotEmpty()) Text(text = t.description, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Срок: ${t.deadline ?: "—"}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Приоритет: ${t.priority}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onToggle) { Text(if (t.completed) "Отменить" else "Готово") }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onDelete) { Text(text = "Удалить") }
            }
        }
    }
}

