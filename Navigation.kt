package com.pkfuturegkgs.hardsecurityguard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pkfuturegkgs.hardsecurityguard.viewmodel.SecurityViewModel

private sealed class Dest(val route: String, val label: String) {
    data object Dashboard : Dest("dashboard", "Dashboard")
    data object History : Dest("history", "History")
    data object Settings : Dest("settings", "Settings")
}

private const val ROUTE_EMERGENCY = "emergency"

@Composable
fun HsgNavHost(viewModel: SecurityViewModel) {
    val navController = rememberNavController()
    val bottomBarDests = listOf(Dest.Dashboard, Dest.History, Dest.Settings)

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination

            NavigationBar {
                bottomBarDests.forEach { dest ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (dest) {
                                    Dest.Dashboard -> Icons.Filled.Security
                                    Dest.History -> Icons.Filled.History
                                    Dest.Settings -> Icons.Filled.Settings
                                },
                                contentDescription = dest.label
                            )
                        },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onOpenEmergency = { navController.navigate(ROUTE_EMERGENCY) }
                )
            }
            composable(ROUTE_EMERGENCY) { EmergencyScreen() }
            composable(Dest.History.route) { HistoryScreen(viewModel = viewModel) }
            composable(Dest.Settings.route) { SettingsScreen(viewModel = viewModel) }
        }
    }
}
