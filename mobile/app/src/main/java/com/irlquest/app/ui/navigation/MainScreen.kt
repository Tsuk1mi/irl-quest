package com.irlquest.app.ui.navigation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.irlquest.app.feature.worldmap.PaperMapScreen
import com.irlquest.app.feature.guilds.GuildsScreen
import com.irlquest.app.feature.coop.CoopMissionsScreen
import com.irlquest.app.feature.settings.SettingsScreen
import com.irlquest.app.feature.auction.AuctionScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val emoji: String = ""
) {
    object Home : BottomNavItem("home", "Таверна", Icons.Default.Home, "")
    object Quests : BottomNavItem("quests", "Квесты", Icons.Default.EmojiEvents, "")
    object Tasks : BottomNavItem("tasks", "Задания", Icons.Default.ListAlt, "")
    object Hero : BottomNavItem("hero", "Герой", Icons.Default.Person, "")
    object WorldMap : BottomNavItem("worldmap", "Карта", Icons.Default.Explore, "")
    object Stats : BottomNavItem("stats", "Статистика", Icons.Default.Insights, "")
    object Guilds : BottomNavItem("guilds", "Гильдии", Icons.Default.Groups, "")
    object Coop : BottomNavItem("coop", "Кооп", Icons.Default.PeopleAlt, "")
    object Auction : BottomNavItem("auction", "Аукцион", Icons.Default.ShoppingCart, "")
    object Settings : BottomNavItem("settings", "Настройки", Icons.Default.Settings, "")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    // Создаём общий authViewModel для всех экранов
    val authViewModel: com.irlquest.app.feature.auth.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = com.irlquest.app.ui.theme.TavernWood,
                contentColor = com.irlquest.app.ui.theme.PrimaryLight
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Оставляем только 5 основных вкладок для удобства навигации
                // Остальные доступны через Home screen или напрямую
                val items = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Quests,
                    BottomNavItem.Hero,
                    BottomNavItem.WorldMap,
                    BottomNavItem.Settings
                )

                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
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
    authViewModel: com.irlquest.app.feature.auth.AuthViewModel,
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
                onNavigateToTasks = { navController.navigate("tasks") },
                onNavigateToGuilds = { navController.navigate(BottomNavItem.Guilds.route) },
                onNavigateToCoop = { navController.navigate(BottomNavItem.Coop.route) },
                onNavigateToAuction = { navController.navigate(BottomNavItem.Auction.route) },
                onNavigateToFocus = { navController.navigate("focus") },
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

        composable("tasks") {
            TasksScreen(
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate("tasks/$taskId")
                },
                authViewModel = authViewModel
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
            PaperMapScreen(
                onQuestClick = { questId ->
                    navController.navigate("quests/$questId")
                }
            )
        }
        
        composable(BottomNavItem.Hero.route) {
            HeroProfileScreen()
        }
        
        composable(BottomNavItem.Stats.route) {
            StatsScreen()
        }

        composable(BottomNavItem.Guilds.route) {
            GuildsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGuildDetail = { guildId ->
                    // TODO: добавить детальный экран гильдии
                }
            )
        }

        composable(BottomNavItem.Coop.route) {
            CoopMissionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMissionDetail = { missionId ->
                    // TODO: добавить детальный экран миссии
                }
            )
        }

        composable(BottomNavItem.Auction.route) {
            AuctionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(BottomNavItem.Settings.route) {
            SettingsScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Focus session route
        composable("focus") {
            FocusSessionScreen()
        }
    }
}
