package com.irlquest.app.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.irlquest.app.TokenStorage
import com.irlquest.app.ui.screens.LoginScreen
import com.irlquest.app.ui.screens.RegisterScreen

object Destinations {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    
    // Проверяем, есть ли сохраненный токен
    val hasToken by remember { 
        derivedStateOf { TokenStorage.getToken()?.isNotEmpty() == true }
    }
    
    val startDestination = if (hasToken) Destinations.MAIN else Destinations.LOGIN

    NavHost(
        navController = navController, 
        startDestination = startDestination, 
        modifier = modifier
    ) {
        composable(Destinations.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Destinations.MAIN) {
                        popUpTo(Destinations.LOGIN) { inclusive = true }
                    }
                }, 
                onRegister = {
                    navController.navigate(Destinations.REGISTER)
                }
            )
        }
        composable(Destinations.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Destinations.MAIN) {
                        popUpTo(Destinations.LOGIN) { inclusive = true }
                    }
                }, 
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Destinations.MAIN) {
            MainScreen()
        }
    }
}
