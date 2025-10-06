package com.irlquest.app.feature.quests

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestEditScreen(
    questId: Int,
    onSaved: () -> Unit = {},
    viewModel: QuestDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(questId) {
        viewModel.loadQuest(questId)
    }

    val quest = uiState.quest
    var title by remember { mutableStateOf(quest?.title ?: "") }
    var description by remember { mutableStateOf(quest?.description ?: "") }
    var difficulty by remember { mutableStateOf(quest?.difficulty ?: 1) }
    // priority as Int (1..4)
    var priority by remember { mutableStateOf(quest?.priority?.let { p ->
        when (p) {
            QuestPriority.LOW -> 1
            QuestPriority.MEDIUM -> 2
            QuestPriority.HIGH -> 3
            QuestPriority.CRITICAL -> 4
        }
    } ?: 2) }
    // status as String expected by updateQuest
    var status by remember { mutableStateOf(quest?.status?.name?.lowercase() ?: "active") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Редактировать квест", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Priority selector
            Text("Приоритет:", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (p in 1..5) {
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text("$p") }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    // вызов с именованными параметрами, без difficulty (не поддерживается)
                    viewModel.updateQuest(id = questId, title = title, description = description, priority = priority, status = status)
                    onSaved()
                }) {
                    Text("Сохранить")
                }

                OutlinedButton(onClick = { onSaved() }) {
                    Text("Отмена")
                }
            }

            uiState.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
