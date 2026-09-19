package com.mikhilnaika.continueapp.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.mikhilnaika.continueapp.core.audio.LocalArcadeAudio
import com.mikhilnaika.continueapp.core.audio.Sfx
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueMotion
import com.mikhilnaika.continueapp.core.design.ContinueShapes
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles

/**
 * A rectangular arcade-styled call-to-action button. For the true circular DRAW button,
 * see [ArcadeDrawButton] — this one is for ordinary CTAs ("INSERT COIN", "GO PRO", etc).
 */
@Composable
fun ArcadeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = ContinueColors.AccentCoin,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val audio = LocalArcadeAudio.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = ContinueMotion.snappy(),
        label = "arcadeButtonScale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(ContinueShapes.RADIUS_BUTTON_DP.dp))
            .background(if (enabled) accent else ContinueColors.OutlineDim)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                // Every arcade button clicks. Payoff sounds (coin, power-up) are layered on by
                // whatever the press *causes*, so this stays a tiny blip.
                onClick = {
                    audio.play(Sfx.BLIP)
                    onClick()
                },
            )
            .padding(PaddingValues(horizontal = 24.dp, vertical = 14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = ContinueTextStyles.label,
            color = if (enabled) ContinueColors.SurfaceVoid else ContinueColors.TextTertiary,
        )
    }
}

/**
 * The DRAW button: a true circle, domed with a coloured glow, that depresses with a spring
 * and haptic on press. Full lever physics live in feature/draw — this is the nav-bar entry
 * point into it (docs/02-PRODUCT-SPEC.md navigation diagram).
 */
@Composable
fun ArcadeDrawButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val audio = LocalArcadeAudio.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = ContinueMotion.snappy(),
        label = "drawButtonScale",
    )

    Box(
        modifier = modifier
            .size(64.dp)
            .scale(scale)
            .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = ContinueColors.AccentCoin, spotColor = ContinueColors.AccentCoin)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(ContinueColors.AccentCoin, Color(0xFFC79A2E)),
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                // Every arcade button clicks. Payoff sounds (coin, power-up) are layered on by
                // whatever the press *causes*, so this stays a tiny blip.
                onClick = {
                    audio.play(Sfx.BLIP)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "DRAW",
            style = ContinueTextStyles.label,
            color = ContinueColors.SurfaceVoid,
        )
    }
}
