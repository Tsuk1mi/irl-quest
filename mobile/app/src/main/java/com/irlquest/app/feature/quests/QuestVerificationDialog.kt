package com.irlquest.app.feature.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.irlquest.app.ui.theme.*
import com.irlquest.shared.models.QuizQuestion
import com.irlquest.shared.models.MLVerificationResponse

/**
 * Диалог верификации выполнения квеста
 * Показывает тест или запрос на фото
 */
@Composable
fun QuestVerificationDialog(
    questTitle: String,
    verification: MLVerificationResponse,
    onQuizSubmit: (List<Int>) -> Unit,
    onPhotoSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "📝 Подтверждение завершения",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "«$questTitle»",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                when (verification.verificationType) {
                    "quiz" -> {
                        verification.quiz?.let { quiz ->
                            QuizVerificationSection(
                                quiz = quiz.questions,
                                onSubmit = onQuizSubmit,
                                onCancel = onDismiss
                            )
                        }
                    }
                    "photo" -> {
                        PhotoVerificationSection(
                            prompt = verification.photoPrompt ?: "Сделайте фото выполненной задачи",
                            requirements = verification.photoRequirements ?: emptyList(),
                            onSubmit = onPhotoSubmit,
                            onCancel = onDismiss
                        )
                    }
                    else -> {
                        // Автоматическое подтверждение
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Success
                            )
                            Text(
                                "Квест завершён!",
                                style = MaterialTheme.typography.titleMedium,
                                color = Success
                            )
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("Продолжить")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizVerificationSection(
    quiz: List<QuizQuestion>,
    onSubmit: (List<Int>) -> Unit,
    onCancel: () -> Unit
) {
    var selectedAnswers by remember { mutableStateOf(List(quiz.size) { -1 }) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Ответьте на вопросы чтобы подтвердить выполнение:",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurface.copy(alpha = 0.8f)
        )

        quiz.forEachIndexed { index, question ->
            QuizQuestionCard(
                questionIndex = index + 1,
                question = question,
                selectedAnswer = selectedAnswers[index],
                onAnswerSelect = { answerIndex ->
                    selectedAnswers = selectedAnswers.toMutableList().also {
                        it[index] = answerIndex
                    }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }
            Button(
                onClick = { onSubmit(selectedAnswers) },
                enabled = selectedAnswers.all { it >= 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Отправить")
            }
        }
    }
}

@Composable
private fun QuizQuestionCard(
    questionIndex: Int,
    question: QuizQuestion,
    selectedAnswer: Int,
    onAnswerSelect: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$questionIndex. ${question.question}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = TavernWood
            )

            question.options.forEachIndexed { index, option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedAnswer == index,
                        onClick = { onAnswerSelect(index) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoVerificationSection(
    prompt: String,
    requirements: List<String>,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Camera,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MysticBlue
        )

        Text(
            text = prompt,
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurface,
            textAlign = TextAlign.Center
        )

        if (requirements.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Info.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Требования:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Info
                )
                requirements.forEach { req ->
                    Text(
                        "• $req",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Text(
            "Фото будет автоматически удалено после обработки для защиты приватности",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }
            Button(
                onClick = {
                    // TODO: Implement camera/gallery picker, convert to base64
                    onSubmit("base64_placeholder")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Загрузить фото")
            }
        }
    }
}

