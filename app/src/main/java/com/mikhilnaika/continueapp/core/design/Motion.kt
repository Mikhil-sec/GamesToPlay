package com.mikhilnaika.continueapp.core.design

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Named spring specs — docs/03-DESIGN-SYSTEM.md §4. Springs, not curves: default to
 * physics for anything the user touches or that should feel weighty.
 */
object ContinueMotion {
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.75f, stiffness = 900f)
    fun <T> card(): SpringSpec<T> = spring(dampingRatio = 0.68f, stiffness = 380f)
    fun <T> heavy(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = 180f)

    const val EMPHASIZED_DURATION_MS = 400
}

/** Radii — docs/03-DESIGN-SYSTEM.md §3. */
object ContinueShapes {
    const val RADIUS_CARD_DP = 14
    const val RADIUS_SHEET_TOP_DP = 24
    const val RADIUS_BUTTON_DP = 12
}

/** Spacing scale, 4dp base grid — not specified numerically in the design doc beyond radii,
 * kept conservative and consistent across screens. */
object ContinueSpacing {
    const val XS = 4
    const val SM = 8
    const val MD = 12
    const val LG = 16
    const val XL = 24
    const val XXL = 32
}
