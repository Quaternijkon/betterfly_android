package com.betterfly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.betterfly.app.ui.AppViewModel
import com.betterfly.app.ui.history.HistoryScreen
import com.betterfly.app.ui.home.HomeScreen
import com.betterfly.app.ui.settings.SettingsScreen
import com.betterfly.app.ui.stats.StatsScreen
import com.betterfly.app.ui.theme.BetterFlyTheme
import dagger.hilt.android.AndroidEntryPoint

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val settings by appViewModel.settings.collectAsState()

            BetterFlyTheme(
                themeColor = settings.themeColor,
                darkMode = settings.darkMode
            ) {
                val navController = rememberNavController()
                val navItems = listOf(
                    NavItem("home", "首页", Icons.Default.Home),
                    NavItem("stats", "统计", Icons.Default.BarChart),
                    NavItem("history", "历史", Icons.Default.History),
                    NavItem("settings", "设置", Icons.Default.Settings)
                )

                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                val currentRoute = currentDestination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            navItems.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                ) { _ ->
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(onNavigateToSettings = { navController.navigate("settings") })
                        }
                        composable("stats") { StatsScreen() }
                        composable("history") { HistoryScreen() }
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    if (currentRoute != "home") navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
