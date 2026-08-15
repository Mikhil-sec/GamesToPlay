package com.mikhilnaika.continueapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles

data class ArcadeNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
)

/**
 * The 4-destination bottom bar with DRAW raised in the centre — docs/02-PRODUCT-SPEC.md
 * navigation diagram: PILE · DISCOVER · (DRAW) · YOU.
 */
@Composable
fun ArcadeScaffold(
    navItemsLeft: List<ArcadeNavItem>,
    navItemsRight: List<ArcadeNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onDrawClick: () -> Unit,
    coinBalance: Int,
    isPro: Boolean,
    showBottomBar: Boolean = true,
    /**
     * The coin balance only earns its 52dp of a phone's screen where coins are actually
     * *about* to matter: DRAW (where they're spent) and YOU (where the account lives). On
     * PILE and DISCOVER it pushed the whole screen down for information nobody was acting on.
     * The status-bar inset is consumed either way — see [ArcadeTopBar].
     */
    showCoinCounter: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        containerColor = ContinueColors.SurfaceVoid,
        topBar = {
            // Shares `showBottomBar`'s visibility: the immersive routes (onboarding, Credits
            // Roll, RANK, share card) are cinematic and must stay chrome-free, and they draw
            // their own status-bar handling.
            if (showBottomBar) {
                ArcadeTopBar(coinBalance = coinBalance, isPro = isPro, showCoinCounter = showCoinCounter)
            }
        },
        bottomBar = {
            if (showBottomBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ContinueColors.SurfaceCabinet)
                        // Background first, inset padding second: the cabinet colour runs
                        // behind the gesture bar while the labels sit clear of it. On a
                        // gesture-nav phone the nav items were otherwise half-swallowed.
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(64.dp),
                ) {
                    navItemsLeft.forEach { item ->
                        NavBarItem(item, currentRoute == item.route, onNavigate, Modifier.weight(1f))
                    }
                    // DRAW lives *inside* the bar as its own column rather than being aligned to
                    // the bar's centre. With two items left and one right the empty middle column
                    // is centred at 62.5% of the width, not 50% — so a centre-aligned button sat
                    // 12.5% of the screen too far left and, on a phone, landed on top of the
                    // DISCOVER label. Making it a real child means it self-centres on its own
                    // column whatever the item counts are.
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        // `offset` (never `padding`) for the raise: padding throws on negative
                        // values — see docs/10-BUILD-STATUS.md §3 bug #9.
                        ArcadeDrawButton(onClick = onDrawClick, modifier = Modifier.offset(y = (-24).dp))
                    }
                    navItemsRight.forEach { item ->
                        NavBarItem(item, currentRoute == item.route, onNavigate, Modifier.weight(1f))
                    }
                }
            }
        },
    ) { padding ->
        content(Modifier.padding(padding))
    }
}

/**
 * The top chrome. Even with the coin counter hidden this still consumes the status-bar inset:
 * the app draws edge-to-edge, so an *absent* top bar would let content slide under the clock
 * rather than simply sit higher.
 */
@Composable
private fun ArcadeTopBar(coinBalance: Int, isPro: Boolean, showCoinCounter: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ContinueColors.SurfaceCabinet)
            .windowInsetsPadding(WindowInsets.statusBars)
            .then(
                if (showCoinCounter) Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                else Modifier,
            ),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCoinCounter) {
            if (isPro) {
                Text(
                    text = "PRO",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.AccentCoin,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            CoinCounter(balance = coinBalance)
        }
    }
}

@Composable
private fun NavBarItem(
    item: ArcadeNavItem,
    selected: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable { onNavigate(item.route) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (selected) ContinueColors.AccentCoin else ContinueColors.TextSecondary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = item.label,
            // Deliberately smaller and tighter than `label` (12sp/0.08em): "DISCOVER" is the
            // widest word in the bar, and at a phone width with the user's font scale turned up
            // it outgrew its own column and spilled into the neighbouring one. One line,
            // ellipsised, clamped to the column — the icon above it carries the meaning anyway.
            style = ContinueTextStyles.label.copy(fontSize = 10.sp, letterSpacing = 0.02.em),
            color = if (selected) ContinueColors.AccentCoin else ContinueColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        )
    }
}
