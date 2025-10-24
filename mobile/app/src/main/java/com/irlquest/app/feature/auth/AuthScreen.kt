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
import com.irlquest.app.ui.viewmodel.AuthViewModel
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
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Вход", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Имя пользователя") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { viewModel.login(username.trim(), password) }, enabled = !isLoading && username.isNotBlank() && password.isNotBlank()) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text(text = "Войти")
                }

                // Dev convenience: quick login for local testing
                if (BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        // Sample dev credentials — change if your local server uses different ones
                        username = "testuser"
                        password = "password"
                        viewModel.login(username, password)
                    }) {
                        Text(text = "Dev: quick login")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { showRegister = true }) {
                    Text(text = "Регистрация")
                }
            }
        }
        }
    }
}
