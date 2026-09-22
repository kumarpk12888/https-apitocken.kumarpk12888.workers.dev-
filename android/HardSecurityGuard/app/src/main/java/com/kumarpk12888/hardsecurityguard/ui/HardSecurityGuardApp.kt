package com.kumarpk12888.hardsecurityguard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kumarpk12888.hardsecurityguard.security.SecurityScanResult
import com.kumarpk12888.hardsecurityguard.ui.screens.AppsScreen
import com.kumarpk12888.hardsecurityguard.ui.screens.DashboardScreen
import com.kumarpk12888.hardsecurityguard.ui.screens.EmergencyScreen
import com.kumarpk12888.hardsecurityguard.ui.screens.HistoryScreen
import com.kumarpk12888.hardsecurityguard.ui.screens.NetworkScreen
import com.kumarpk12888.hardsecurityguard.ui.screens.PermissionScreen
import com.kumarpk12888.hardsecurityguard.ui.screens.SettingsScreen
import com.kumarpk12888.hardsecurityguard.ui.viewmodel.SecurityViewModel

@Composable
fun HardSecurityGuardApp(
    navController: NavHostController = rememberNavController(),
    viewModel: SecurityViewModel = viewModel()
) {
    val scanResult by viewModel.scanResult.collectAsState()
    val history by viewModel.history.collectAsState(initial = emptyList())

    Surface(color = MaterialTheme.colorScheme.background) {
        NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    scanResult = scanResult,
                    history = history,
                    navController = navController,
                    onRunScan = { viewModel.runScan() },
                    onSync = { viewModel.syncNow() }
                )
            }
            composable(Screen.Apps.route) {
                AppsScreen(scanResult = scanResult, navController = navController)
            }
            composable(Screen.Permissions.route) {
                PermissionScreen(scanResult = scanResult, navController = navController)
            }
            composable(Screen.Network.route) {
                NetworkScreen(scanResult = scanResult, navController = navController)
            }
            composable(Screen.History.route) {
                HistoryScreen(history = history, navController = navController, onClear = { viewModel.clearHistory() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }
            composable(Screen.Emergency.route) {
                EmergencyScreen(navController = navController)
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Apps : Screen("apps")
    object Permissions : Screen("permissions")
    object Network : Screen("network")
    object History : Screen("history")
    object Settings : Screen("settings")
    object Emergency : Screen("emergency")
}
