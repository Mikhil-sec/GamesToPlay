package com.mikhilnaika.continueapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
    showBottomBar: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        containerColor = ContinueColors.SurfaceVoid,
        bottomBar = {
            if (showBottomBar) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(ContinueColors.SurfaceCabinet),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                    ) {
                        navItemsLeft.forEach { item ->
                            NavBarItem(item, currentRoute == item.route, onNavigate, Modifier.weight(1f))
                        }
                        // Spacer under the raised DRAW button.
                        Box(modifier = Modifier.weight(1f))
                        navItemsRight.forEach { item ->
                            NavBarItem(item, currentRoute == item.route, onNavigate, Modifier.weight(1f))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-24).dp),
                    ) {
                        ArcadeDrawButton(onClick = onDrawClick)
                    }
                }
            }
        },
    ) { padding ->
        content(Modifier.padding(padding))
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
            style = ContinueTextStyles.label,
            color = if (selected) ContinueColors.AccentCoin else ContinueColors.TextSecondary,
        )
    }
}
