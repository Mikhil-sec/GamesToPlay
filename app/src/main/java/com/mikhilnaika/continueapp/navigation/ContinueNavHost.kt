package com.mikhilnaika.continueapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mikhilnaika.continueapp.core.ui.ArcadeNavItem
import com.mikhilnaika.continueapp.core.ui.ArcadeScaffold
import com.mikhilnaika.continueapp.feature.completion.CreditsRollScreen
import com.mikhilnaika.continueapp.feature.discover.DiscoverScreen
import com.mikhilnaika.continueapp.feature.draw.DrawScreen
import com.mikhilnaika.continueapp.feature.onboarding.OnboardingScreen
import com.mikhilnaika.continueapp.feature.paywall.PaywallScreen
import com.mikhilnaika.continueapp.feature.pile.PileScreen
import com.mikhilnaika.continueapp.feature.profile.ProfileScreen
import com.mikhilnaika.continueapp.feature.rank.RankScreen
import com.mikhilnaika.continueapp.feature.share.PileShareScreen
import com.mikhilnaika.continueapp.feature.stacks.StacksScreen

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
    chromeViewModel: AppChromeViewModel = hiltViewModel(),
) {
    val coinBalance by chromeViewModel.coinBalance.collectAsState()
    val isPro by chromeViewModel.isPro.collectAsState()
    val startDestination = if (onboardingComplete) NavDestinations.PILE else NavDestinations.ONBOARDING
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val fullScreenRoutes = setOf(
        NavDestinations.ONBOARDING,
        NavDestinations.CREDITS_ROLL,
        NavDestinations.RANK,
        NavDestinations.SHARE_PILE,
        // RevenueCatUI draws its own full-bleed layout including a close button; leaving our
        // chrome on top of it would double up the dismiss affordances.
        NavDestinations.PAYWALL,
    )
    val showBottomBar = currentRoute != null && currentRoute !in fullScreenRoutes
    // The coin balance is only chrome where coins are the subject: DRAW spends them, YOU is the
    // account screen. Everywhere else it cost a phone ~52dp of header for a number nobody was
    // about to act on, and pushed PILE's tabs visibly down the screen.
    val coinCounterRoutes = setOf(NavDestinations.DRAW, NavDestinations.PROFILE)
    val showCoinCounter = currentRoute in coinCounterRoutes

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
        coinBalance = coinBalance,
        isPro = isPro,
        showBottomBar = showBottomBar,
        showCoinCounter = showCoinCounter,
    ) { padding ->
        NavHost(navController = navController, startDestination = startDestination, modifier = padding) {
            composable(NavDestinations.ONBOARDING) {
                OnboardingScreen(onFinished = { startAtDiscover ->
                    val destination =
                        if (startAtDiscover) NavDestinations.DISCOVER else NavDestinations.PILE
                    navController.navigate(destination) {
                        popUpTo(NavDestinations.ONBOARDING) { inclusive = true }
                    }
                })
            }
            composable(NavDestinations.PILE) {
                PileScreen(
                    onGameCompleted = { entryId ->
                        navController.navigate(NavDestinations.creditsRoll(entryId))
                    },
                    onOpenStacks = { navController.navigate(NavDestinations.STACKS) },
                    onOpenShare = { navController.navigate(NavDestinations.SHARE_PILE) },
                )
            }
            composable(NavDestinations.STACKS) { StacksScreen() }
            composable(NavDestinations.SHARE_PILE) { PileShareScreen(onDismiss = { navController.popBackStack() }) }
            composable(NavDestinations.DISCOVER) { DiscoverScreen() }
            composable(NavDestinations.DRAW) {
                DrawScreen(
                    onNavigateToDiscover = {
                        navController.navigate(NavDestinations.DISCOVER) { launchSingleTop = true }
                    },
                    onGoPro = { navController.navigate(NavDestinations.PAYWALL) },
                )
            }
            composable(NavDestinations.PROFILE) {
                ProfileScreen(onGoPro = { navController.navigate(NavDestinations.PAYWALL) })
            }
            composable(NavDestinations.PAYWALL) {
                PaywallScreen(onDismiss = { navController.popBackStack() })
            }
            composable(
                NavDestinations.CREDITS_ROLL,
                arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
            ) {
                CreditsRollScreen(
                    onRankIt = { gameId ->
                        navController.navigate(NavDestinations.rank(gameId)) {
                            popUpTo(NavDestinations.PILE)
                        }
                    },
                    onSkipToPile = {
                        navController.navigate(NavDestinations.PILE) { popUpTo(NavDestinations.PILE) { inclusive = true } }
                    },
                )
            }
            composable(
                NavDestinations.RANK,
                arguments = listOf(navArgument("gameId") { type = NavType.LongType }),
            ) {
                RankScreen(onFinished = {
                    navController.navigate(NavDestinations.PILE) { popUpTo(NavDestinations.PILE) { inclusive = true } }
                })
            }
        }
    }
}
