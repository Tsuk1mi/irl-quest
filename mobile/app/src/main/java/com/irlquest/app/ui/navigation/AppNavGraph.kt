package com.irlquest.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.irlquest.app.TokenStorage
import com.irlquest.app.ui.screens.LoginScreen
import com.irlquest.app.ui.screens.RegisterScreen
import com.irlquest.app.ui.screens.TasksScreen
import com.irlquest.app.ui.screens.QuestsScreen

object Destinations {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val TASKS = "tasks"
    const val QUESTS = "quests"
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Destinations.LOGIN, modifier = modifier) {
        composable(Destinations.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Destinations.TASKS) {
                    popUpTo(Destinations.LOGIN) { inclusive = true }
                }
            }, onRegister = {
                navController.navigate(Destinations.REGISTER)
            })
        }
        composable(Destinations.REGISTER) {
            RegisterScreen(onRegisterSuccess = {
                navController.navigate(Destinations.TASKS) {
                    popUpTo(Destinations.LOGIN) { inclusive = true }
                }
            }, onBack = {
                navController.popBackStack()
            })
        }
        composable(Destinations.TASKS) {
            TasksScreen(onNavigateToQuests = { navController.navigate(Destinations.QUESTS) }, onLogout = {
                // при логауте возвращаемся на экран входа
                TokenStorage.clear()
                navController.navigate(Destinations.LOGIN) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            })
        }
        composable(Destinations.QUESTS) {
            QuestsScreen()
        }
    }
}
