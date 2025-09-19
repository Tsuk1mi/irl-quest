package com.irlquest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.irlquest.app.ui.screens.QuestGeneratorScreen
import com.irlquest.app.ui.theme.IRLQuestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            IRLQuestTheme {
                IRLQuestApp()
            }
        }
    }
}

@Composable
fun IRLQuestApp() {
    val navController = rememberNavController()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IRL Quest") },
                backgroundColor = MaterialTheme.colors.primary
            )
        },
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                val items = listOf(
                    Triple("home", "Home", Icons.Default.Home),
                    Triple("generator", "Generator", Icons.Default.AutoFixHigh),
                    Triple("profile", "Profile", Icons.Default.Person)
                )
                
                items.forEach { (route, label, icon) ->
                    BottomNavigationItem(
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "generator",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
    // Placeholder for home screen
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Text(
            text = "Home Screen - Coming Soon!",
            style = MaterialTheme.typography.h4
        )
    }
}

@Composable
fun ProfileScreen() {
    // Placeholder for profile screen  
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Text(
            text = "Profile Screen - Coming Soon!",
            style = MaterialTheme.typography.h4
        )
    }
}