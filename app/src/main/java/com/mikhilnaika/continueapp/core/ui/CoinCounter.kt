package com.mikhilnaika.continueapp.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueMotion
import com.mikhilnaika.continueapp.core.design.monoStyle

/**
 * The reused mascot motion (docs/03-DESIGN-SYSTEM.md §4.5): the coin spins on its Y axis
 * whenever the balance changes. Reused everywhere a balance is earned, spent, or shown.
 */
@Composable
fun CoinCounter(
    balance: Int,
    modifier: Modifier = Modifier,
) {
    var rotation by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = ContinueMotion.card(),
        label = "coinSpin",
    )

    LaunchedEffect(balance) {
        rotation += 360f
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Circle,
            contentDescription = "Coins",
            tint = ContinueColors.AccentCoin,
            modifier = Modifier
                .rotate(animatedRotation)
                .width(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = balance.toString(),
            style = monoStyle(size = 17.sp),
            color = ContinueColors.TextPrimary,
        )
    }
}
