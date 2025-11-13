package com.irlquest.app.feature.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.irlquest.app.ui.theme.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.shared.models.MLQuestGenerationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class QuestGeneratorUiState(
    val isGenerating: Boolean = false,
    val generatedQuest: MLQuestGenerationResponse? = null,
    val error: String? = null
)

class QuestGeneratorViewModel : ViewModel() {
    private val mlRepo = com.irlquest.app.data.repository.MLRepository()

    private val _uiState = MutableStateFlow(QuestGeneratorUiState())
    val uiState: StateFlow<QuestGeneratorUiState> = _uiState

    fun generateQuest(todoText: String, context: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
            try {
                val result = mlRepo.generateQuest(todoText, context)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generatedQuest = result
                )
            } catch (e: Exception) {
                Timber.e(e, "QuestGeneratorViewModel: failed to generate quest")
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Ошибка генерации квеста"
                )
            }
        }
    }

    fun clearGenerated() {
        _uiState.value = _uiState.value.copy(generatedQuest = null)
    }
}

/**
 * Диалог ML-генерации квеста с автоопределением сложности
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestGeneratorDialog(
    onDismiss: () -> Unit,
    onAccept: (MLQuestGenerationResponse) -> Unit,
    viewModel: QuestGeneratorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var todoText by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MagicPurple
                )

                Text(
                    text = "✨ Генерация квеста с ИИ",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    textAlign = TextAlign.Center
                )

                if (uiState.generatedQuest == null) {
                    // Форма ввода
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = todoText,
                            onValueChange = { todoText = it },
                            label = { Text("Что нужно сделать?") },
                            placeholder = { Text("Например: Изучить Kotlin") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                focusedLabelColor = Primary
                            )
                        )

                        OutlinedTextField(
                            value = context,
                            onValueChange = { context = it },
                            label = { Text("Дополнительный контекст (опционально)") },
                            placeholder = { Text("Детали или пожелания...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                focusedLabelColor = Primary
                            )
                        )

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MysticBlue.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🤖",
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    "ИИ автоматически определит сложность, создаст задачи и назначит награды",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Отмена")
                            }
                            Button(
                                onClick = {
                                    viewModel.generateQuest(todoText, context.ifBlank { null })
                                },
                                enabled = todoText.isNotBlank() && !uiState.isGenerating,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                if (uiState.isGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Генерировать")
                                }
                            }
                        }
                    }
                } else {
                    // Показ сгенерированного квеста
                    val generated = uiState.generatedQuest!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = generated.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TavernWood
                        )
                        Text(
                            text = generated.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface.copy(alpha = 0.8f)
                        )

                        Divider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Сложность: ${generated.difficulty}/5", fontWeight = FontWeight.Bold)
                            Text("Награда: ${generated.rewardExperience} XP", fontWeight = FontWeight.Bold, color = MysticBlue)
                        }

                        Text("Задачи:", fontWeight = FontWeight.Bold, color = TavernWood)
                        generated.tasks.forEach { task ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", fontSize = 16.sp)
                                Column {
                                    Text(task.title, style = MaterialTheme.typography.bodyMedium)
                                    if (task.description.isNotBlank()) {
                                        Text(
                                            task.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.clearGenerated()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = {
                                onAccept(generated)
                                viewModel.clearGenerated()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Success)
                        ) {
                            Text("Принять")
                        }
                    }
                }

                uiState.error?.let { error ->
                    Text(
                        text = "Ошибка: $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

