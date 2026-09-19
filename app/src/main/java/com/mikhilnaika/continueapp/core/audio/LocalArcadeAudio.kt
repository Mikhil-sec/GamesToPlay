package com.mikhilnaika.continueapp.core.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * The one [ArcadeAudio] instance, handed down the tree — the same arrangement as
 * [com.mikhilnaika.continueapp.core.ui.LocalHaptics], so no screen can build an audio engine of
 * its own that the SOUND and MUSIC toggles don't reach.
 */
val LocalArcadeAudio = staticCompositionLocalOf<ArcadeAudio> {
    error("LocalArcadeAudio not provided — wrap the content in CompositionLocalProvider from your Activity")
}

/**
 * Plays [track] while this composable is on screen *and* the app is in the foreground.
 *
 * Tied to the lifecycle rather than to composition alone because composition outlives
 * visibility: without the ON_STOP half, pressing home on the CONTINUE? screen would leave its
 * countdown loop playing over the launcher.
 */
@Composable
fun MusicCue(track: MusicTrack, preview: MusicPack? = null) {
    val audio = LocalArcadeAudio.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(track, preview, lifecycleOwner) {
        // Adding an observer to a lifecycle that is already STARTED replays ON_START, so this
        // also covers the ordinary case of a screen composed while the app is visible.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> audio.startMusic(track, preview)
                Lifecycle.Event.ON_STOP -> audio.stopMusic(track, preview)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audio.stopMusic(track, preview)
        }
    }
}
