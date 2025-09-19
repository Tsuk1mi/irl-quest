package com.irlquest.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.irlquest.app.feature.focus.FocusSessionScreen
import com.irlquest.app.feature.quests.QuestsScreen
import com.irlquest.app.feature.stats.StatsScreen
import com.irlquest.app.feature.tasks.TasksScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Tasks : BottomNavItem("tasks", "Задачи", Icons.Default.Assignment)
    object Quests : BottomNavItem("quests", "Квесты", Icons.Default.EmojiEvents)
    object Focus : BottomNavItem("focus", "Фокус", Icons.Default.Timer)
    object Stats : BottomNavItem("stats", "Статистика", Icons.Default.Analytics)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigation(
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.primary
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                val items = listOf(
                    BottomNavItem.Tasks,
                    BottomNavItem.Quests,
                    BottomNavItem.Focus,
                    BottomNavItem.Stats
                )
                
                items.forEach { item ->
                    BottomNavigationItem(
                        icon = { 
                            Icon(
                                item.icon, 
                                contentDescription = item.title
                            ) 
                        },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        MainNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Tasks.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Tasks.route) {
            TasksScreen()
        }
        composable(BottomNavItem.Quests.route) {
            QuestsScreen(
                onNavigateToQuestDetail = { questId ->
                    // TODO: Navigate to quest detail
                }
            )
        }
        composable(BottomNavItem.Focus.route) {
            FocusSessionScreen()
        }
        composable(BottomNavItem.Stats.route) {
            StatsScreen()
        }
    }
}