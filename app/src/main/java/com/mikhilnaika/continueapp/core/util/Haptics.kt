package com.mikhilnaika.continueapp.core.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

/**
 * Thin wrapper over the platform vibrator — docs/03-DESIGN-SYSTEM.md §5. Global on/off is a
 * settings toggle at the call site; this class only knows how to fire an effect.
 */
class Haptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService<VibratorManager>()?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService<Vibrator>()
    }

    fun light() = oneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)
    fun medium() = oneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
    fun heavy() = oneShot(50, 255)

    /** docs/03-DESIGN-SYSTEM.md — "Game cleared: custom waveform, a short celebratory pattern." */
    fun celebratory() {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 40, 60, 40, 60, 80)
            val amplitudes = intArrayOf(0, 120, 0, 180, 0, 255)
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }

    private fun oneShot(durationMs: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }
}
