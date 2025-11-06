package com.irlquest.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irlquest.app.ui.theme.*

/**
 * 📜 КАРТОЧКА КВЕСТА - Стиль фэнтези-пергамента
 * 
 * Отображает квест как "объявление на доске" в стиле таверны
 */
@Composable
fun QuestCard(
    title: String,
    description: String? = null,
    difficulty: Int = 1,
    progress: Int = 0,
    totalTasks: Int = 1,
    xpReward: Int = 0,
    goldReward: Int = 0,
    deadline: String? = null,
    isLegendary: Boolean = false,
    onClick: () -> Unit = {}
) {
    // Определяем цвет границы по сложности
    val borderColor = when (difficulty) {
        1 -> QuestBronze
        2 -> QuestSilver
        3, 4 -> QuestGold
        else -> QuestLegendary
    }

    // Эмодзи для сложности
    val difficultyIcon = when (difficulty) {
        1 -> "🥉"
        2 -> "🥈"
        3, 4 -> "🥇"
        else -> "⚡"
    }

    // Анимация свечения для легендарных квестов
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(
                elevation = if (isLegendary) 8.dp else 4.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (isLegendary) QuestLegendary.copy(alpha = glowAlpha) else Color.Black
            )
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Заголовок с иконкой сложности
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = difficultyIcon,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }

                // Дедлайн (если есть)
                deadline?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = Warning.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Warning
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = Warning,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Описание (если есть)
            description?.let {
                if (it.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface.copy(alpha = 0.8f),
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Прогресс
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📋 Прогресс: $progress/$totalTasks",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = OnSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${(progress * 100 / totalTasks.coerceAtLeast(1))}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Полоса прогресса
                LinearProgressIndicator(
                    progress = (progress.toFloat() / totalTasks.coerceAtLeast(1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        progress == totalTasks -> Success
                        progress >= totalTasks * 0.7f -> Primary
                        else -> SecondaryLight
                    },
                    trackColor = Surface
                )
            }

            // Награды
            if (xpReward > 0 || goldReward > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // XP
                    if (xpReward > 0) {
                        RewardChip(
                            icon = "⭐",
                            label = "+$xpReward XP",
                            color = MysticBlue
                        )
                    }

                    // Золото
                    if (goldReward > 0) {
                        RewardChip(
                            icon = "💰",
                            label = "+$goldReward",
                            color = Primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 🏅 ЧИП НАГРАДЫ
 */
@Composable
private fun RewardChip(
    icon: String,
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = icon,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 📋 КОМПАКТНАЯ КАРТОЧКА КВЕСТА (для горизонтального списка)
 */
@Composable
fun CompactQuestCard(
    title: String,
    difficulty: Int = 1,
    progress: Int,
    totalTasks: Int,
    onClick: () -> Unit = {}
) {
    val difficultyColor = when (difficulty) {
        1 -> QuestBronze
        2 -> QuestSilver
        3, 4 -> QuestGold
        else -> QuestLegendary
    }

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            difficultyColor.copy(alpha = 0.1f),
                            Surface
                        )
                    )
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                color = OnSurface
            )

            Column {
                LinearProgressIndicator(
                    progress = (progress.toFloat() / totalTasks.coerceAtLeast(1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = difficultyColor,
                    trackColor = Surface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$progress/$totalTasks задач",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

