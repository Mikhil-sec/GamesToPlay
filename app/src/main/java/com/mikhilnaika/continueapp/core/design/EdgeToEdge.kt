package com.mikhilnaika.continueapp.core.design

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * Edge-to-edge for every Activity in the app — Android 15 forces it for targetSdk 35+, and Play
 * Console flags any Activity that doesn't opt in explicitly.
 *
 * **Always the dark style, never `auto`.** The no-argument `enableEdgeToEdge()` picks icon colour
 * from the *system* theme, so on a phone in light mode it drew dark status-bar icons over this
 * dark-only app's dark chrome — the clock and battery were effectively invisible. CONTINUE? has
 * one theme, so the bars get one style.
 *
 * The navigation bar keeps a translucent scrim, which only shows with 3-button navigation (gesture
 * navigation stays fully transparent); without it the buttons would sit on raw content.
 *
 * This replaces the `android:statusBarColor` / `android:navigationBarColor` theme attributes,
 * which Android 15 deprecates and ignores for apps drawing edge-to-edge.
 */
fun ComponentActivity.enableArcadeEdgeToEdge() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(NAVIGATION_SCRIM),
    )
}

/** The Activity library's own default dark scrim (`#801B1B1B`). */
private val NAVIGATION_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
