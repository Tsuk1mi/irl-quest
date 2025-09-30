package com.irlquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.data.network.dto.QuestGenerationRequest
import com.irlquest.app.ui.viewmodels.QuestGeneratorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestGeneratorScreen(
    viewModel: QuestGeneratorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    var todoText by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf("fantasy") }
    var selectedDifficulty by remember { mutableStateOf(3) }
    var context by remember { mutableStateOf("") }
    
    val themes = listOf(
        "fantasy" to "🧙‍♂️ Fantasy",
        "sci-fi" to "🚀 Sci-Fi", 
        "modern" to "💼 Modern",
        "medieval" to "⚔️ Medieval"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "IRL Quest Generator",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        // Todo текст
        OutlinedTextField(
            value = todoText,
            onValueChange = { todoText = it },
            label = { Text("Что нужно сделать?") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            maxLines = 3
        )

        // Выбор темы
        Text(
            text = "Выберите тему",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themes.forEach { (id, name) ->
                FilterChip(
                    selected = selectedTheme == id,
                    onClick = { selectedTheme = id },
                    label = { Text(name) }
                )
            }
        }

        // Выбор сложности
        Text(
            text = "Сложность",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..5).forEach { difficulty ->
                FilterChip(
                    selected = selectedDifficulty == difficulty,
                    onClick = { selectedDifficulty = difficulty },
                    label = { Text("$difficulty") }
                )
            }
        }

        // Контекст
        OutlinedTextField(
            value = context,
            onValueChange = { context = it },
            label = { Text("Дополнительный контекст (необязательно)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            maxLines = 3
        )

        // Кнопка генерации
        Button(
            onClick = {
                coroutineScope.launch {
                    viewModel.generateQuest(
                        QuestGenerationRequest(
                            todo = todoText,
                            theme = selectedTheme,
                            difficulty = selectedDifficulty,
                            context = context.takeIf { it.isNotBlank() }
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = todoText.isNotBlank()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Сгенерировать квест")
            }
        }

        // Результат
        uiState.quest?.let { quest ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = quest.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Задачи квеста
                    quest.tasks.forEachIndexed { index, task ->
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(task)
                        }
                    }
                }
            }
        }

        // Ошибка
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}