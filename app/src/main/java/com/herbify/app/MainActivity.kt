package com.herbify.app
import com.herbify.app.data.local.HerbifyDatabase
import com.herbify.app.data.HerbariumRepository
import com.herbify.app.viewmodel.HerbariumViewModel
import com.herbify.app.viewmodel.HerbariumViewModelFactory
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.herbify.app.game.GameState
import com.herbify.app.model.OsmZoneRepository
import com.herbify.app.screens.CollectionScreen
import com.herbify.app.screens.EventsScreen
import com.herbify.app.screens.MapScreen
import com.herbify.app.screens.OnboardingScreen
import com.herbify.app.screens.ProfileScreen
import com.herbify.app.screens.ScannerScreen
import com.herbify.app.screens.ShopScreen
import com.herbify.app.screens.WalletScreen
import com.herbify.app.ui.theme.DarkBg
import com.herbify.app.ui.theme.DarkSurface
import com.herbify.app.ui.theme.HerbifyTheme
import com.herbify.app.ui.theme.NeonGreen
import com.herbify.app.ui.theme.TextSecondary

sealed class Screen(val route: String, val label: String, val emoji: String) {
    data object Onboarding : Screen("onboarding", "Onboarding", "🌿")
    data object Map : Screen("map", "Map", "🗺️")
    data object Scanner : Screen("scanner/{zoneId}", "Scanner", "📷") {
        fun withZone(zoneId: String) = "scanner/$zoneId"
        const val tabRoute = "scanner/none"
    }
    data object Collection : Screen("collection", "Collection", "🌿")
    data object Shop : Screen("shop", "Shop", "🛒")
    data object Profile : Screen("profile", "Profile", "👤")
    data object Events : Screen("events", "Events", "🌍")
    data object Wallet : Screen("wallet", "Wallet", "💰")
}

val bottomNavItems = listOf(
    Screen.Map,
    Screen.Scanner,
    Screen.Collection,
    Screen.Shop,
    Screen.Profile
)

fun isFirstLaunch(context: Context): Boolean =
    context.getSharedPreferences("herbify_prefs", Context.MODE_PRIVATE)
        .getBoolean("is_first_launch", true)

fun markOnboardingComplete(context: Context) =
    context.getSharedPreferences("herbify_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("is_first_launch", false)
        .apply()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HerbifyTheme {
                HerbifyApp()
            }
        }
    }
}
@Composable
fun HerbifyApp() {
    val navController = rememberNavController()
    val gameState: GameState = viewModel()
    val context = LocalContext.current

    val db = remember { HerbifyDatabase.getInstance(context) }
    val herbariumRepository = remember { HerbariumRepository(db.capturedPlantDao()) }
    val herbariumFactory = remember { HerbariumViewModelFactory(herbariumRepository) }
    val herbariumViewModel: HerbariumViewModel = viewModel(factory = herbariumFactory)

    val startDestination = remember {
        if (isFirstLaunch(context)) Screen.Onboarding.route else Screen.Map.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = remember(currentRoute) {
        currentRoute == Screen.Map.route ||
                currentRoute == Screen.Scanner.tabRoute ||
                currentRoute == Screen.Collection.route ||
                currentRoute == Screen.Shop.route ||
                currentRoute == Screen.Profile.route
    }

    Scaffold(
        containerColor = DarkBg,
        bottomBar = {
            if (showBottomBar) {
                HerbifyBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        val targetRoute = if (screen == Screen.Scanner) {
                            Screen.Scanner.tabRoute
                        } else {
                            screen.route
                        }

                        navController.navigate(targetRoute) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        markOnboardingComplete(context)
                        navController.navigate(Screen.Map.route) {
                            popUpTo(Screen.Onboarding.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(Screen.Map.route) {
                MapScreen(
                    gameState = gameState,
                    onNavigateToScanner = { zone ->
                        navController.navigate(Screen.Scanner.withZone(zone.id))
                    }
                )
            }

            composable(
                route = Screen.Scanner.route,
                arguments = listOf(navArgument("zoneId") { type = NavType.StringType })
            ) { backStackEntry ->
                val zoneId = backStackEntry.arguments?.getString("zoneId")
                val zone = remember(zoneId) {
                    if (zoneId == null || zoneId == "none") {
                        null
                    } else {
                        OsmZoneRepository.getCachedZones().find { zoneItem ->
                            zoneItem.id == zoneId
                        }
                    }
                }

                ScannerScreen(
                    gameState = gameState,
                    zone = zone,
                    herbariumViewModel = herbariumViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Collection.route) {
                CollectionScreen(viewModel = herbariumViewModel)
            }

            composable(Screen.Shop.route) {
                ShopScreen(gameState = gameState)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    gameState = gameState,
                    onNavigateToEvents = { navController.navigate(Screen.Events.route) },
                    onNavigateToWallet = { navController.navigate(Screen.Wallet.route) }
                )
            }

            composable(Screen.Events.route) {
                EventsScreen(
                    gameState = gameState,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Wallet.route) {
                WalletScreen(
                    gameState = gameState,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun HerbifyBottomBar(currentRoute: String?, onNavigate: (Screen) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(
                width = 1.dp,
                color = NeonGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { screen ->
                val isSelected = when (screen) {
                    Screen.Scanner -> currentRoute?.startsWith("scanner/") == true
                    else -> currentRoute == screen.route
                }

                BottomNavItem(
                    screen = screen,
                    isSelected = isSelected,
                    onClick = { onNavigate(screen) }
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(screen: Screen, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (isSelected) NeonGreen.copy(alpha = 0.11f) else Color.Transparent,
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 16.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(screen.emoji, fontSize = 20.sp)
            Text(
                text = screen.label,
                color = if (isSelected) NeonGreen else TextSecondary,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                letterSpacing = if (isSelected) 0.3.sp else 0.sp
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(NeonGreen, RoundedCornerShape(2.dp))
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}