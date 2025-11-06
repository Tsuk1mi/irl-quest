package com.irlquest.app.feature.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.BuildConfig
import com.irlquest.app.feature.auth.AuthViewModel
import com.irlquest.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showRegister by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        if (currentUser != null) onLoginSuccess()
    }

    // Show error in snackbar when it appears using coroutineScope
    LaunchedEffect(error) {
        error?.let { msg ->
            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        if (showRegister) {
            RegisterScreen(onRegistered = {
                showRegister = false
            }, viewModel = viewModel)
        } else {
            // 🏰 ФЭНТЕЗИ ЭКРАН ЛОГИНА - ВХОД В ТАВЕРНУ
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                TavernWood.copy(alpha = 0.3f),
                                Background,
                                Background
                            )
                        )
                    )
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 🏰 Заголовок таверны
                    Text(
                        text = "🏰",
                        fontSize = 72.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "IRL QUEST",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "⚔️ Таверна Героев ⚔️",
                        style = MaterialTheme.typography.titleMedium,
                        color = TavernWood,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // Карточка входа в стиле пергамента
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(16.dp))
                            .border(
                                width = 2.dp,
                                color = Primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📜 Вход в Гильдию",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = TavernWood
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            // Поле имени с иконкой
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("⚔️ Имя героя") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Primary)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    focusedLabelColor = Primary,
                                    cursorColor = Primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Поле пароля с иконкой
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("🔐 Секретное слово") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Primary)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    focusedLabelColor = Primary,
                                    cursorColor = Primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Кнопка входа в фэнтези-стиле
                            Button(
                                onClick = { viewModel.login(username.trim(), password) },
                                enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Primary,
                                    contentColor = OnPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = OnPrimary
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "⚔️ Войти в таверну",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Dev кнопка быстрого входа
                            if (BuildConfig.DEBUG) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        username = "testuser"
                                        password = "password"
                                        viewModel.login(username, password)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Secondary
                                    )
                                ) {
                                    Text(text = "🧙 DEV: Быстрый вход")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Divider(color = TavernWood.copy(alpha = 0.3f))

                            Spacer(modifier = Modifier.height(16.dp))

                            // Кнопка регистрации
                            TextButton(
                                onClick = { showRegister = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📝 Зарегистрировать нового героя",
                                    color = Secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Мотивационная цитата NPC
                    Text(
                        text = "\"Каждый великий герой начинал с малого\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = TavernWood.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }
}
