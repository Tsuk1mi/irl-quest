package com.irlquest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.irlquest.app.feature.auth.AuthScreen
import com.irlquest.app.ui.viewmodel.AuthViewModel
import com.irlquest.app.ui.screens.QuestGeneratorScreen
import com.irlquest.app.ui.theme.IRLQuestTheme
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Инициализация TokenStorage для использования в TokenInterceptor
        TokenStorage.init(applicationContext)

        // Инициализация логирования (Timber + файл)
        AppLogger.init(applicationContext)

        // Перехват необработанных исключений для локального сбора стектрейса в файл
        val previousDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log via Timber (this will also be written to the file tree)
            try {
                Timber.e(throwable, "Uncaught exception on thread %s", thread.name)
            } catch (_: Exception) {
                // ignore
            }
            try {
                val file = File(cacheDir, "crash_log.txt")
                file.appendText("\n---- CRASH ${System.currentTimeMillis()} ----\n")
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                file.appendText(sw.toString())
            } catch (e: Exception) {
                // ignore
            }
            // Delegate to previous default handler
            previousDefaultHandler?.uncaughtException(thread, throwable)
        }

        setContent {
            IRLQuestTheme {
                MainEntry()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEntry() {
    val authViewModel: AuthViewModel = viewModel()
    // Попробовать получить профиль при запуске (если есть токен)
    LaunchedEffect(Unit) {
        authViewModel.fetchMe()
    }

    val currentUser by authViewModel.currentUser.collectAsState()

    if (currentUser == null) {
        // Показываем экран логина/регистрации
        // Передаём тот же экземпляр viewModel, чтобы AuthScreen и MainEntry разделяли состояние
        AuthScreen(onLoginSuccess = { authViewModel.fetchMe() }, viewModel = authViewModel)
    } else {
        IRLQuestApp()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IRLQuestApp() {
    val navController = rememberNavController()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IRL Quest") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                val items = listOf(
                    Triple("home", "Home", Icons.Default.Home),
                    Triple("generator", "Generator", Icons.Default.AutoFixHigh),
                    Triple("profile", "Profile", Icons.Default.Person)
                )
                
                items.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "generator",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen()
            }
            
            composable("generator") {
                QuestGeneratorScreen()
            }
            
            composable("profile") {
                ProfileScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = "Home Screen - Coming Soon!",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
fun ProfileScreen() {
    val authViewModel: com.irlquest.app.ui.viewmodel.AuthViewModel = viewModel()
    val user by authViewModel.currentUser.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            user?.let { u ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Простая иконка вместо изображения аватара
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = u.username.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(text = u.username, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = u.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Уровень: ${u.level}", style = MaterialTheme.typography.bodyLarge)
                        Text(text = "Опыт: ${u.experience}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Последний вход: ${u.lastLogin ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { authViewModel.fetchMe() }) {
                        Text("Обновить")
                    }
                    OutlinedButton(onClick = { authViewModel.logout() }) {
                        Text("Выйти")
                    }
                }
            } ?: run {
                Text(text = error ?: "Профиль не загружен", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}