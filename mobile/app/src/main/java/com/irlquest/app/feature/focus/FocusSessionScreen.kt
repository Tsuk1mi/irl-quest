package com.irlquest.app.feature.focus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@Composable
fun FocusSessionScreen(
    viewModel: FocusViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadFocusSessions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Фокус-сессии",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Активная сессия или кнопка запуска
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = if (uiState.activeSession != null) MaterialTheme.colors.primary else Color.White
        ) {
            if (uiState.activeSession != null) {
                ActiveSessionCard(
                    session = uiState.activeSession,
                    onStop = { viewModel.stopSession() }
                )
            } else {
                StartSessionCard(
                    onStart = { duration -> viewModel.startSession(duration) }
                )
            }
        }

        // Статистика
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard("Сегодня", "${uiState.todayStats.focusSessions}")
            StatCard("Всего минут", "${uiState.todayStats.totalMinutes}")
            StatCard("Продуктивность", "${uiState.todayStats.productivity}%")
        }

        // История сессий
        Text(
            text = "История сессий",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                items(uiState.sessions) { session ->
                    FocusSessionItem(session = session)
                    Divider()
                }
            }
        }
    }
}

@Composable
fun ActiveSessionCard(
    session: FocusSessionUi,
    onStop: () -> Unit
) {
    var timeRemaining by remember { mutableStateOf(session.durationMinutes * 60) }
    
    LaunchedEffect(session) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
    }
    
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Timer,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = formatTime(timeRemaining),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Text(
            text = session.taskTitle ?: "Фокус-сессия",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Завершить", color = MaterialTheme.colors.primary)
        }
    }
}

@Composable
fun StartSessionCard(
    onStart: (Int) -> Unit
) {
    var selectedDuration by remember { mutableStateOf(25) }
    val durations = listOf(15, 25, 45, 60)
    
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Начать фокус-сессию",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            durations.forEach { duration ->
                FilterChip(
                    selected = selectedDuration == duration,
                    onClick = { selectedDuration = duration },
                    content = { Text("${duration} мин") }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onStart(selectedDuration) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Начать сессию")
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.size(width = 100.dp, height = 80.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun FocusSessionItem(session: FocusSessionUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Timer,
            contentDescription = null,
            tint = MaterialTheme.colors.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.taskTitle ?: "Фокус-сессия",
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${session.durationMinutes} мин • ${session.formattedDate}",
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
        
        session.productivityRating?.let { rating ->
            Card(
                backgroundColor = when {
                    rating >= 4 -> Color.Green.copy(alpha = 0.1f)
                    rating >= 3 -> Color.Yellow.copy(alpha = 0.1f)
                    else -> Color.Red.copy(alpha = 0.1f)
                }
            ) {
                Text(
                    text = "★ $rating",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}