package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.HoneyGold
import com.example.ui.theme.NavyBg
import com.example.ui.theme.SlateCard
import com.example.ui.theme.ThriftHiveTheme
import com.example.ui.viewmodel.ThriftViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ThriftViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkMode.collectAsState()

            ThriftHiveTheme(darkTheme = isDarkTheme) {
                ThriftHiveApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThriftHiveApp(viewModel: ThriftViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Decide if we should show the bottom bar based on current screen route
    val showBottomBar = currentRoute in listOf(
        "dashboard",
        "inventory",
        "returns",
        "losses",
        "reports"
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SlateCard, // Redefined Onyx color
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    val items = listOf(
                        BottomNavItem("Dashboard", "dashboard", Icons.Default.Dashboard),
                        BottomNavItem("Inventory", "inventory", Icons.Default.Inventory2),
                        BottomNavItem("Returns", "returns", Icons.AutoMirrored.Filled.CompareArrows),
                        BottomNavItem("Losses", "losses", Icons.AutoMirrored.Filled.TrendingDown),
                        BottomNavItem("Reports", "reports", Icons.Default.BarChart)
                    )

                    items.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (selected) Color.Black else Color(0xFF94A3B8)
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    color = if (selected) HoneyGold else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = HoneyGold
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Quick action settings entry FAB on Dashboard
            if (currentRoute == "dashboard") {
                FloatingActionButton(
                    onClick = { navController.navigate("settings") },
                    containerColor = HoneyGold,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings configuration")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 300 },
                    animationSpec = tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(350))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -300 },
                    animationSpec = tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(350))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -300 },
                    animationSpec = tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(350))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 300 },
                    animationSpec = tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(350))
            }
        ) {
            composable("splash") {
                val currentUser by viewModel.currentUserEmail.collectAsState()
                SplashScreen(onNavigateToDashboard = {
                    val startDest = if (currentUser == null) "login" else "dashboard"
                    navController.navigate(startDest) {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }

            composable("login") {
                AuthScreen(
                    viewModel = viewModel,
                    onAuthSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddProduct = { navController.navigate("add_edit_product") },
                    onNavigateToReturnCalc = { navController.navigate("returns") },
                    onNavigateToLossCalc = { navController.navigate("losses") },
                    onNavigateToOrders = { navController.navigate("orders") },
                    onNavigateToProductDetails = { uuid ->
                        navController.navigate("add_edit_product?productId=$uuid")
                    }
                )
            }

            composable("inventory") {
                InventoryScreen(
                    viewModel = viewModel,
                    onNavigateToAddProduct = { navController.navigate("add_edit_product") },
                    onNavigateToEditProduct = { uuid ->
                        navController.navigate("add_edit_product?productId=$uuid")
                    }
                )
            }

            composable("returns") {
                ReturnCalculatorScreen(viewModel = viewModel)
            }

            composable("losses") {
                LossCalculatorScreen(viewModel = viewModel)
            }

            composable("reports") {
                ProfitReportScreen(viewModel = viewModel)
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToTeamSharing = { navController.navigate("team_sharing") },
                    onLogOutSuccess = {
                        navController.navigate("login") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                )
            }

            composable("team_sharing") {
                TeamSharingScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("orders") {
                com.example.ui.screens.OrdersScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "add_edit_product?productId={productId}",
                arguments = listOf(
                    navArgument("productId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val productIdArg = backStackEntry.arguments?.getString("productId")
                AddEditProductScreen(
                    viewModel = viewModel,
                    productIdToEdit = productIdArg,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
