package com.mikhilnaika.continueapp.core.design

import androidx.compose.ui.graphics.Color

/**
 * Neo-arcade palette. Dark-only — see docs/03-DESIGN-SYSTEM.md.
 * Pure black is banned; everything sits on a slightly blue-shifted near-black.
 */
object ContinueColors {
    val SurfaceVoid = Color(0xFF08090C)
    val SurfaceCabinet = Color(0xFF101218)
    val SurfaceRaised = Color(0xFF181B23)
    val SurfaceFelt = Color(0xFF0E1A16)
    val OutlineDim = Color(0xFF242833)

    val AccentCoin = Color(0xFFF7C948)
    val AccentNeon = Color(0xFF00E5A0)
    val AccentHot = Color(0xFFFF3D7F)
    val AccentCool = Color(0xFF5B8CFF)

    val TextPrimary = Color(0xFFF2F4F8)
    val TextSecondary = Color(0xFF9AA3B2)
    val TextTertiary = Color(0xFF5A6373)

    val HairlineLight = Color(0x0DFFFFFF) // #FFFFFF0D — the top-edge hairline
}
