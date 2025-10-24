package com.irlquest.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.irlquest.app.feature.focus.FocusSessionScreen
import com.irlquest.app.feature.home.HomeScreen
import com.irlquest.app.feature.quests.QuestsScreen
import com.irlquest.app.feature.quests.QuestDetailScreen
import com.irlquest.app.feature.stats.StatsScreen
import com.irlquest.app.feature.tasks.TasksScreen
import com.irlquest.app.feature.tasks.TaskDetailScreen
import com.irlquest.app.feature.hero.HeroProfileScreen
import com.irlquest.app.feature.worldmap.WorldMapScreen
import com.irlquest.app.ui.theme.Orange

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "Таверна", Icons.Default.Home)
    object Quests : BottomNavItem("quests", "Квесты", Icons.Default.EmojiEvents)
    object WorldMap : BottomNavItem("worldmap", "Карта", Icons.Default.Map)
    object Hero : BottomNavItem("hero", "Герой", Icons.Default.Person)
    object Stats : BottomNavItem("stats", "Статистика", Icons.Default.Analytics)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val items = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Quests,
                    BottomNavItem.WorldMap,
                    BottomNavItem.Hero,
                    BottomNavItem.Stats
                )

                items.forEach { item ->
                    NavigationBarItem(
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
                                popUpTo(navController.graph.startDestinationId) {
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
        startDestination = BottomNavItem.Home.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen(
                onNavigateToQuest = { questId -> navController.navigate("quests/$questId") },
                onNavigateToTask = { taskId -> navController.navigate("tasks/$taskId") }
            )
        }

        composable(BottomNavItem.Quests.route) {
            QuestsScreen(
                onNavigateToQuestDetail = { questId ->
                    navController.navigate("quests/$questId")
                }
            )
        }

        // Detail route with integer argument
        composable(
            route = "quests/{questId}",
            arguments = listOf(navArgument("questId") { type = NavType.IntType })
        ) { backStackEntry ->
            val questId = backStackEntry.arguments?.getInt("questId") ?: 0
            QuestDetailScreen(questId = questId, onTaskClick = { taskId ->
                navController.navigate("tasks/$taskId")
            })
        }

        // Task detail route
        composable(
            route = "tasks/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.IntType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId") ?: 0
            TaskDetailScreen(taskId = taskId, onDeleted = { navController.popBackStack() })
        }

        composable(BottomNavItem.WorldMap.route) {
            WorldMapScreen()
        }
        
        composable(BottomNavItem.Hero.route) {
            HeroProfileScreen()
        }
        
        composable(BottomNavItem.Stats.route) {
            StatsScreen()
        }
    }
}
