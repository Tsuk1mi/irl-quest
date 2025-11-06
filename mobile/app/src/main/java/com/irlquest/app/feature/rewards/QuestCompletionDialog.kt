package com.irlquest.app.feature.rewards

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.irlquest.app.ui.theme.*
import kotlinx.coroutines.delay

data class QuestReward(
    val experienceGained: Int,
    val goldGained: Int,
    val questTitle: String,
    val levelUp: Boolean = false,
    val newLevel: Int = 1
)

@Composable
fun QuestCompletionDialog(
    reward: QuestReward,
    onDismiss: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    var showGold by remember { mutableStateOf(false) }
    var showXP by remember { mutableStateOf(false) }
    var showLevelUp by remember { mutableStateOf(false) }

    // Анимация появления с задержкой
    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
        delay(500)
        showXP = true
        delay(400)
        showGold = true
        if (reward.levelUp) {
            delay(600)
            showLevelUp = true
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box {
                // Фон с градиентом
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Primary.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Анимированная иконка успеха
                    AnimatedSuccessIcon(visible = showContent)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Заголовок
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + expandVertically()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "✨ Квест Завершен! ✨",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = reward.questTitle,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Награды
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Опыт
                        AnimatedRewardCard(
                            visible = showXP,
                            icon = "🧠",
                            title = "Получен опыт",
                            value = "+${reward.experienceGained} XP",
                            color = Secondary
                        )

                        // Золото
                        AnimatedRewardCard(
                            visible = showGold,
                            icon = "💰",
                            title = "Получено золота",
                            value = "+${reward.goldGained}",
                            color = Primary
                        )
                    }

                    // Повышение уровня
                    if (reward.levelUp) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AnimatedLevelUpCard(
                            visible = showLevelUp,
                            newLevel = reward.newLevel
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Кнопка закрытия
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = OnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Продолжить приключение",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedSuccessIcon(visible: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "success_icon_scale"
    )

    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "success_icon_rotation"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(scale)
            .rotate(if (visible) rotation else 0f)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        Primary.copy(alpha = 0.3f),
                        Primary.copy(alpha = 0.1f)
                    )
                )
            )
            .border(4.dp, Primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🏆",
            fontSize = 56.sp
        )
    }
}

@Composable
private fun AnimatedRewardCard(
    visible: Boolean,
    icon: String,
    title: String,
    value: String,
    color: Color
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy
            )
        ) + fadeIn()
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = icon, fontSize = 32.sp)
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun AnimatedLevelUpCard(
    visible: Boolean,
    newLevel: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "level_up_scale"
    )

    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_animation"
    )

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy
            )
        ) + fadeIn()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp,
            color = MagicPurple.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .border(
                    2.dp,
                    Brush.horizontalGradient(
                        listOf(
                            MagicPurple.copy(alpha = 0.5f + shimmer * 0.5f),
                            Primary.copy(alpha = 0.5f + shimmer * 0.5f),
                            MagicPurple.copy(alpha = 0.5f + shimmer * 0.5f)
                        )
                    ),
                    RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "👑",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ПОВЫШЕНИЕ УРОВНЯ!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MagicPurple
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Новый уровень: $newLevel",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Гильдия гордится тобой, герой!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Превью компонента для тестирования
@Composable
fun QuestCompletionDialogPreview() {
    IRLQuestTheme {
        QuestCompletionDialog(
            reward = QuestReward(
                experienceGained = 100,
                goldGained = 50,
                questTitle = "Изучить Kotlin Compose",
                levelUp = true,
                newLevel = 5
            ),
            onDismiss = {}
        )
    }
}

