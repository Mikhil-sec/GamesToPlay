package com.mikhilnaika.continueapp.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikhilnaika.continueapp.core.audio.LocalArcadeAudio
import com.mikhilnaika.continueapp.core.audio.Sfx
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * The FREE PLAY clock in the top bar — "FREE PLAY 42:17". An arcade in free-play mode said so
 * on the marquee; this is the same promise made visible, so the hour is felt as a gift that's
 * running rather than a setting that silently expires.
 */
@Composable
fun FreePlayBadge(endsAtMillis: Long, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAtMillis) {
        while (now < endsAtMillis) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val remaining = ((endsAtMillis - now) / 1000).coerceAtLeast(0)
    Text(
        text = "FREE PLAY %d:%02d".format(remaining / 60, remaining % 60),
        style = ContinueTextStyles.label,
        color = ContinueColors.AccentHot,
        modifier = modifier,
    )
}

/**
 * "+50 COINS" dropping in from the top, wherever the user is — the visible half of the
 * RevenueCat coin grant. PRO's 50-coin drops arrive on purchase and on every renewal, usually
 * when the app next comes to the foreground, and the coin counter only shows on DRAW and YOU;
 * without this a paid perk would land in silence on whatever screen happened to be open.
 *
 * Small grants (a verified ad's single coin) don't get a banner: the counter on the screen
 * that earned them already spins and chimes.
 */
@Composable
fun CoinDropBanner(drops: Flow<Int>, modifier: Modifier = Modifier) {
    val audio = LocalArcadeAudio.current
    var shown by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(drops) {
        drops.collect { amount ->
            if (amount < BIG_DROP) return@collect
            shown = amount
            audio.play(Sfx.COIN_DROP)
            delay(3_200)
            shown = null
        }
    }
    AnimatedVisibility(
        visible = shown != null,
        modifier = modifier.windowInsetsPadding(WindowInsets.statusBars),
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ContinueColors.SurfaceRaised)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "+${shown ?: 0} COINS",
                    style = ContinueTextStyles.monoL,
                    color = ContinueColors.AccentCoin,
                )
                Text(
                    text = "COIN DROP · LANDED IN YOUR CABINET",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** At or above this, a grant is a PRO drop and gets the banner (and the coin counter stays quiet). */
const val BIG_DROP = 10
