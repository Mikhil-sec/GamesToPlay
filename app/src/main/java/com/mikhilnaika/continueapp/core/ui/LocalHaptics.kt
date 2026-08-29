package com.mikhilnaika.continueapp.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.mikhilnaika.continueapp.core.util.Haptics

/**
 * The one [Haptics] instance, handed down the tree.
 *
 * Screens used to write `remember { Haptics(context) }`, which quietly made every screen its
 * own haptics engine — and meant the HAPTICS setting (which only the injected singleton can
 * see) applied to none of them. A composition local keeps the ergonomics of the old line while
 * making it impossible to obtain an instance that isn't the configured one.
 *
 * `static` because the value never changes for the life of an Activity: reads shouldn't be
 * tracked, and a change would be a whole-subtree invalidation for nothing.
 */
val LocalHaptics = staticCompositionLocalOf<Haptics> {
    error("LocalHaptics not provided — wrap the content in CompositionLocalProvider from your Activity")
}
