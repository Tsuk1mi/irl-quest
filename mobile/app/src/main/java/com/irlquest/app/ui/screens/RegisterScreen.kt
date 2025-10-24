package com.irlquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onBack: () -> Unit = {}, viewModel: AuthViewModel = viewModel()) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Notify caller when registration succeeded and currentUser becomes available
    LaunchedEffect(currentUser) {
        if (currentUser != null) onRegisterSuccess()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Register", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        if (error != null) {
            Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(onClick = { viewModel.register(email.trim(), username.trim(), password) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary) else Text("Register")
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { onBack() }) { Text("Back") }
    }
}
