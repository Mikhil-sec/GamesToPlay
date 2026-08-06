package com.mikhilnaika.continueapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mikhilnaika.continueapp.core.ui.ArcadeNavItem
import com.mikhilnaika.continueapp.core.ui.ArcadeScaffold
import com.mikhilnaika.continueapp.feature.discover.DiscoverScreen
import com.mikhilnaika.continueapp.feature.draw.DrawPlaceholderScreen
import com.mikhilnaika.continueapp.feature.onboarding.OnboardingScreen
import com.mikhilnaika.continueapp.feature.pile.PileScreen
import com.mikhilnaika.continueapp.feature.profile.ProfileScreen

/**
 * Root nav graph. Onboarding gates everything else (docs/02-PRODUCT-SPEC.md §8) and is
 * skipped on subsequent launches once onboarding has already completed. The bottom bar
 * hides itself during onboarding rather than living in a separate nav graph, since that
 * keeps exactly one NavHost/NavController alive for the whole app.
 */
@Composable
fun ContinueNavHost(
    onboardingComplete: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination = if (onboardingComplete) NavDestinations.PILE else NavDestinations.ONBOARDING
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && currentRoute != NavDestinations.ONBOARDING

    ArcadeScaffold(
        navItemsLeft = listOf(
            ArcadeNavItem("PILE", Icons.Filled.Inventory2, NavDestinations.PILE),
            ArcadeNavItem("DISCOVER", Icons.Filled.Search, NavDestinations.DISCOVER),
        ),
        navItemsRight = listOf(
            ArcadeNavItem("YOU", Icons.Filled.Person, NavDestinations.PROFILE),
        ),
        currentRoute = currentRoute,
        onNavigate = { route ->
            navController.navigate(route) { launchSingleTop = true }
        },
        onDrawClick = { navController.navigate(NavDestinations.DRAW) { launchSingleTop = true } },
        showBottomBar = showBottomBar,
    ) { padding ->
        NavHost(navController = navController, startDestination = startDestination, modifier = padding) {
            composable(NavDestinations.ONBOARDING) {
                OnboardingScreen(onFinished = {
                    navController.navigate(NavDestinations.PILE) {
                        popUpTo(NavDestinations.ONBOARDING) { inclusive = true }
                    }
                })
            }
            composable(NavDestinations.PILE) { PileScreen() }
            composable(NavDestinations.DISCOVER) { DiscoverScreen() }
            composable(NavDestinations.DRAW) { DrawPlaceholderScreen() }
            composable(NavDestinations.PROFILE) { ProfileScreen() }
        }
    }
}
