package com.riset.mangodefendd.navigation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.riset.mangodefendd.ui.screens.DashboardScreen
import com.riset.mangodefendd.ui.screens.HistoryScreen
import com.riset.mangodefendd.ui.screens.LoginScreen
import com.riset.mangodefendd.ui.screens.PermissionRequestScreen
import com.riset.mangodefendd.ui.screens.ProfileScreen
import com.riset.mangodefendd.ui.screens.ScanScreen
import com.riset.mangodefendd.ui.screens.TransactionHistoryScreen

object Routes {
    const val Login = "login"
    const val Permission = "permission"
    const val Dashboard = "dashboard"
    const val Scan = "scan"
    const val ScanFolder = "scan_folder"
    const val History = "history"
    const val Subscriptions = "subscriptions"
    const val Transactions = "transactions"
    const val Profile = "profile"
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.Dashboard
) {
    Surface(modifier = modifier) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(Routes.Dashboard) {
                DashboardScreen(
                    onNavigateToScan = { navController.navigate(Routes.Scan) },
                    onNavigateToScanFolder = { navController.navigate(Routes.ScanFolder) },
                    onNavigateToHistory = { navController.navigate(Routes.History) },
                    onNavigateToSubscriptions = { navController.navigate(Routes.Subscriptions) },
                    onNavigateToProfile = { navController.navigate(Routes.Profile) },
                    onLogout = {
                        // When logout, we just stay on dashboard but in guest mode
                        navController.navigate(Routes.Dashboard) {
                            popUpTo(Routes.Dashboard) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Login) {
                LoginScreen(onLoginSuccess = {
                    navController.navigate(Routes.Dashboard) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                })
            }

            composable(Routes.Scan) {
                ScanScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Routes.ScanFolder) {
                // We'll create this screen
                com.riset.mangodefendd.ui.screens.ScanFolderScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Routes.History) {
                HistoryScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Routes.Subscriptions) {
                com.riset.mangodefendd.ui.screens.SubscriptionScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Routes.Transactions) {
                TransactionHistoryScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Routes.Profile) {
                ProfileScreen(
                    onNavigateToHome = {
                        navController.navigate(Routes.Dashboard) {
                            popUpTo(Routes.Dashboard) { inclusive = true }
                        }
                    },
                    onNavigateToHistory = { navController.navigate(Routes.History) },
                    onNavigateToPricing = { navController.navigate(Routes.Subscriptions) },
                    onNavigateToTransactions = { navController.navigate(Routes.Transactions) },
                    onLogout = {
                        // When logout, we just stay on dashboard but in guest mode
                        navController.navigate(Routes.Dashboard) {
                            popUpTo(Routes.Dashboard) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}


