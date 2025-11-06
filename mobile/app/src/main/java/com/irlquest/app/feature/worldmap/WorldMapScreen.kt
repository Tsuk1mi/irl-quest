package com.irlquest.app.feature.worldmap

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irlquest.app.ui.theme.*

data class WorldZone(
    val id: Int,
    val name: String,
    val description: String,
    val category: String,
    val icon: String,
    val completionPercentage: Int,
    val totalQuests: Int,
    val completedQuests: Int,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldMapScreen() {
    var selectedZone by remember { mutableStateOf<WorldZone?>(null) }

    // Пример данных зон
    val zones = remember {
        listOf(
            WorldZone(
                1,
                "🏢 Город Дел",
                "Место, где выполняются рабочие задачи и профессиональные квесты. " +
                        "Здесь кипит деловая активность, и каждая выполненная задача " +
                        "приближает тебя к мастерству.",
                "work",
                "🏢",
                45,
                12,
                5,
                Color(0xFF1976D2)
            ),
            WorldZone(
                2,
                "📚 Гора Знаний",
                "Священное место обучения и развития навыков. Поднимайся все выше " +
                        "по склонам мудрости, изучая новые дисциплины и совершенствуя " +
                        "свои способности.",
                "study",
                "📚",
                60,
                8,
                5,
                MagicPurple
            ),
            WorldZone(
                3,
                "🌲 Лес Спокойствия",
                "Тихая локация для отдыха и хобби. В этих умиротворяющих зарослях " +
                        "ты можешь заниматься любимыми делами и восстанавливать силы.",
                "hobby",
                "🌲",
                30,
                15,
                4,
                Secondary
            ),
            WorldZone(
                4,
                "💪 Храм Здоровья",
                "Святилище, посвященное физическому и ментальному здоровью. " +
                        "Тренировки, медитации и забота о себе делают тебя сильнее.",
                "health",
                "💪",
                25,
                10,
                2,
                Color(0xFFE91E63)
            ),
            WorldZone(
                5,
                "⚠️ Пещера Хаоса",
                "Темное место, где скапливаются просроченные задачи. Чем больше " +
                        "нерешенных дел, тем глубже и опаснее становится пещера. " +
                        "Очисти её, чтобы вернуть свет!",
                "overdue",
                "⚠️",
                80,
                3,
                0,
                Error
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "🗺️ Карта Мира",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TavernWood,
                    titleContentColor = PrimaryLight
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "🗺️ Карта Приключений",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TavernWood
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"Каждая зона представляет сферу твоей жизни. Выполняй квесты, чтобы захватить территории!\"",
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = TavernWood.copy(alpha = 0.7f)
                    )
                }

                items(zones) { zone ->
                    WorldZoneCard(
                        zone = zone,
                        isExpanded = selectedZone?.id == zone.id,
                        onClick = {
                            selectedZone = if (selectedZone?.id == zone.id) null else zone
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorldZoneCard(
    zone: WorldZone,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Заголовок зоны
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(zone.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = zone.icon, fontSize = 32.sp)
                    }

                    Column {
                        Text(
                            text = zone.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${zone.completedQuests}/${zone.totalQuests} квестов",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Прогресс зоны
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Освоено",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${zone.completionPercentage}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = zone.color
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = zone.completionPercentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = zone.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Расширенная информация
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = zone.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Статус зоны
                ZoneStatus(zone)

                Spacer(modifier = Modifier.height(12.dp))

                // Кнопка действия
                Button(
                    onClick = { 
                        // Пока просто сворачиваем - навигация к квестам зоны в v2.1
                        onClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = zone.color,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Свернуть зону")
                }
            }
        }
    }
}

@Composable
private fun ZoneStatus(zone: WorldZone) {
    val status = when {
        zone.completionPercentage >= 90 -> Triple("🏆", "Почти освоена!", Primary)
        zone.completionPercentage >= 60 -> Triple("⚔️", "Идет завоевание", Secondary)
        zone.completionPercentage >= 30 -> Triple("🔍", "Исследуется", Warning)
        else -> Triple("🗺️", "Не освоена", MaterialTheme.colorScheme.outline)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = status.third.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = status.first, fontSize = 24.sp)
            Column {
                Text(
                    text = "Статус зоны",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = status.second,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = status.third
                )
            }
        }
    }
}

