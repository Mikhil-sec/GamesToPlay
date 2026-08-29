package com.mikhilnaika.continueapp.core.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService
import com.mikhilnaika.continueapp.core.data.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over the platform vibrator — docs/03-DESIGN-SYSTEM.md §5.
 *
 * **The HAPTICS toggle in YOU is enforced here, and only here.** It used to be enforced
 * nowhere: this class carried a comment saying "global on/off is a settings toggle at the call
 * site", and not one of the five call sites checked it. `UserPreferencesRepository` faithfully
 * stored the switch, `ProfileScreen` faithfully drew it, and every screen went on constructing
 * its own `Haptics(context)` and buzzing regardless — reported from closed testing as the STACK
 * flick and the DRAW lever both firing with haptics switched off. Making the class a singleton
 * that reads the preference itself is what makes the toggle unfalsifiable: there is no way to
 * fire an effect that skips the check, because there is no other way to fire an effect.
 *
 * Held as a `@Volatile` mirror of the preference flow rather than read per call, because these
 * fire from gesture handlers and animation callbacks where suspending isn't an option.
 */
@Singleton
class Haptics @Inject constructor(
    @ApplicationContext context: Context,
    userPreferencesRepository: UserPreferencesRepository,
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService<VibratorManager>()?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService<Vibrator>()
    }

    @Volatile
    private var enabled: Boolean = true

    init {
        userPreferencesRepository.isHapticsEnabled
            .onEach { enabled = it }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }

    fun light() = oneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)
    fun medium() = oneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
    fun heavy() = oneShot(50, 255)

    /** docs/03-DESIGN-SYSTEM.md — "Game cleared: custom waveform, a short celebratory pattern." */
    fun celebratory() {
        val v = vibrator ?: return
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 40, 60, 40, 60, 80)
            val amplitudes = intArrayOf(0, 120, 0, 180, 0, 255)
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }

    private fun oneShot(durationMs: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }
}
