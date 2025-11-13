package com.irlquest.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.data.settings.UserSettingsStore
import com.irlquest.app.feature.auth.AuthViewModel
import com.irlquest.app.ui.theme.OnSurface
import com.irlquest.app.ui.theme.Primary
import com.irlquest.app.ui.theme.Surface
import com.irlquest.app.ui.theme.TavernWood
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val allowLocalNetwork by UserSettingsStore.allowLocalNetwork.collectAsState()
    val notificationsEnabled by UserSettingsStore.notificationsEnabled.collectAsState()
    val errorMessage by authViewModel.error.collectAsState()

    var nickname by remember(currentUser) { mutableStateOf(currentUser?.username.orEmpty()) }
    var avatarUrl by remember(currentUser) { mutableStateOf(currentUser?.avatarUrl.orEmpty()) }
    var bio by remember(currentUser) { mutableStateOf(currentUser?.bio.orEmpty()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { statusMessage = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TavernWood
                )
            )
        },
        containerColor = Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Профиль героя",
                    style = MaterialTheme.typography.titleMedium,
                    color = TavernWood
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Никнейм") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    label = { Text("URL аватара") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://") }
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Биография") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        authViewModel.updateProfile(
                            username = nickname.ifBlank { null },
                            avatarUrl = avatarUrl.ifBlank { null },
                            bio = bio.ifBlank { null },
                            onSuccess = { statusMessage = "Профиль обновлён" },
                            onError = { statusMessage = it }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить изменения")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Уведомления",
                    style = MaterialTheme.typography.titleMedium,
                    color = TavernWood
                )
                SettingsSwitchRow(
                    title = "Напоминать о ежедневных заданиях",
                    checked = notificationsEnabled,
                    description = "Получайте push-уведомления с ежедневными квестами и отчётами о прогрессе",
                    onCheckedChange = { enabled ->
                        UserSettingsStore.setNotifications(enabled)
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Сеть",
                    style = MaterialTheme.typography.titleMedium,
                    color = TavernWood
                )
                SettingsSwitchRow(
                    title = "Разрешить приватные IP (локальные сервера)",
                    checked = allowLocalNetwork,
                    description = "Отключает блокировку внутренних адресов (192.168.x.x, 10.x.x.x) для тестирования и локального back-end",
                    onCheckedChange = { enabled ->
                        UserSettingsStore.setAllowLocalNetwork(enabled)
                        statusMessage = if (enabled) {
                            "Локальные IP доступны. Перезапустите сетевые операции."
                        } else {
                            "Локальные IP заблокированы."
                        }
                    }
                )
            }

            statusMessage?.let { message ->
                Text(
                    text = message,
                    color = OnSurface.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { statusMessage = null }) {
                    Text("Скрыть")
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    description: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurface.copy(alpha = 0.7f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

