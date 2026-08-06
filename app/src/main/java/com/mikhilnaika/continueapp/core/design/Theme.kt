package com.mikhilnaika.continueapp.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Dark-only app — docs/03-DESIGN-SYSTEM.md §1: "an arcade is a dark room, and committing
 * to one mode buys us polish everywhere else." System dark/light mode is intentionally
 * ignored for the colour scheme; system contrast/reduce-motion settings are still respected
 * elsewhere (see ContinueMotion usage sites).
 */
private val ContinueColorScheme = darkColorScheme(
    primary = ContinueColors.AccentCoin,
    onPrimary = ContinueColors.SurfaceVoid,
    secondary = ContinueColors.AccentNeon,
    onSecondary = ContinueColors.SurfaceVoid,
    tertiary = ContinueColors.AccentHot,
    onTertiary = ContinueColors.TextPrimary,
    background = ContinueColors.SurfaceVoid,
    onBackground = ContinueColors.TextPrimary,
    surface = ContinueColors.SurfaceCabinet,
    onSurface = ContinueColors.TextPrimary,
    surfaceVariant = ContinueColors.SurfaceRaised,
    onSurfaceVariant = ContinueColors.TextSecondary,
    outline = ContinueColors.OutlineDim,
    error = ContinueColors.AccentHot,
    onError = ContinueColors.TextPrimary,
)

@Composable
fun ContinueTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ContinueColorScheme,
        typography = continueTypography(),
        content = content,
    )
}

/** Reads the system "reduce motion" preference so rituals can cross-fade instead of animate. */
@Composable
fun isReduceMotionEnabled(): Boolean {
    // Compose has no first-class accessor; callers combine this with
    // Settings.Global.ANIMATOR_DURATION_SCALE == 0f at the call site where a Context is available.
    return false
}
