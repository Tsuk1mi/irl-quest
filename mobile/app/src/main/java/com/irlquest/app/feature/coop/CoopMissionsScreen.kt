package com.irlquest.app.feature.coop

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.ui.theme.*

// Вспомогательные функции для статусов (определяем до использования)
fun getStatusText(status: String): String {
    return when (status) {
        "recruiting" -> "Идет набор участников"
        "in_progress" -> "Миссия выполняется"
        "completed" -> "Миссия завершена"
        "failed" -> "Миссия провалена"
        else -> status
    }
}

@Composable
fun getStatusColor(status: String): androidx.compose.ui.graphics.Color {
    return when (status) {
        "recruiting" -> Primary
        "in_progress" -> Secondary
        "completed" -> Success
        "failed" -> Error
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "recruiting" -> Pair(Primary, "Набор")
        "in_progress" -> Pair(Secondary, "В процессе")
        "completed" -> Pair(Success, "Завершена")
        "failed" -> Pair(Error, "Провалена")
        else -> Pair(MaterialTheme.colorScheme.outline, status)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoopMissionsScreen(
    viewModel: CoopMissionsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToMissionDetail: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadMissions()
    }

    // Показываем ошибки
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Кооп-миссии",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
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
                .padding(padding)
        ) {
            // Snackbar для ошибок
            androidx.compose.material3.SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Выполняйте квесты вместе с другими игроками!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(uiState.missions) { mission ->
                        CoopMissionCard(
                            mission = mission,
                            onClick = { onNavigateToMissionDetail(mission.id) },
                            onJoin = { viewModel.joinMission(mission.id, "support") }
                        )
                    }

                    if (uiState.missions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Secondary.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "⚔️ Кооперативные миссии",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TavernWood
                                    )
                                    Text(
                                        text = "Функция мультиплеерных миссий находится в разработке на стороне сервера.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Text(
                                        text = "Скоро вы сможете объединяться с другими игроками для выполнения эпических заданий!",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoopMissionCard(
    mission: com.irlquest.app.data.network.dto.CoopMissionDto,
    onClick: () -> Unit,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .clip(RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = mission.quest?.title ?: "Квест #${mission.questId}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TavernWood
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = getStatusText(mission.status),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = getStatusColor(mission.status)
                        )
                    }
                    StatusBadge(status = mission.status)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${mission.partySize}/${mission.maxPartySize}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TavernWood
                            )
                        }
                    }

                    if (mission.status == "recruiting" && mission.partySize < mission.maxPartySize) {
                        Button(
                            onClick = onJoin,
                            modifier = Modifier.height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Secondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Присоединиться",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
