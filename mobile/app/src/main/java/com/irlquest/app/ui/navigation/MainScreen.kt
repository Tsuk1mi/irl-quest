package com.irlquest.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val emoji: String = ""
) {
    object Home : BottomNavItem("home", "Таверна", Icons.Default.Home, "🏰")
    object Quests : BottomNavItem("quests", "Квесты", Icons.Default.EmojiEvents, "📜")
    object Hero : BottomNavItem("hero", "Герой", Icons.Default.Person, "⚔️")
    object WorldMap : BottomNavItem("worldmap", "Карта", Icons.Default.Map, "🗺️")
    object Stats : BottomNavItem("stats", "Статистика", Icons.Default.Analytics, "📊")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    // Создаём общий authViewModel для всех экранов
    val authViewModel: com.irlquest.app.ui.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = com.irlquest.app.ui.theme.TavernWood,
                contentColor = com.irlquest.app.ui.theme.PrimaryLight
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val items = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Quests,
                    BottomNavItem.Hero,
                    BottomNavItem.WorldMap,
                    BottomNavItem.Stats
                )

                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Text(
                                text = item.emoji,
                                fontSize = 24.sp
                            )
                        },
                        label = { 
                            Text(
                                item.title,
                                fontSize = 11.sp,
                                fontWeight = if (currentDestination?.hierarchy?.any { it.route == item.route } == true) 
                                    FontWeight.Bold 
                                else 
                                    FontWeight.Normal
                            ) 
                        },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = com.irlquest.app.ui.theme.Primary,
                            selectedTextColor = com.irlquest.app.ui.theme.Primary,
                            unselectedIconColor = com.irlquest.app.ui.theme.CandleLight,
                            unselectedTextColor = com.irlquest.app.ui.theme.CandleLight.copy(alpha = 0.7f),
                            indicatorColor = com.irlquest.app.ui.theme.Primary.copy(alpha = 0.2f)
                        ),
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
            authViewModel = authViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun MainNavHost(
    navController: NavHostController,
    authViewModel: com.irlquest.app.ui.viewmodel.AuthViewModel,
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
                onNavigateToTask = { taskId -> navController.navigate("tasks/$taskId") },
                authViewModel = authViewModel
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
