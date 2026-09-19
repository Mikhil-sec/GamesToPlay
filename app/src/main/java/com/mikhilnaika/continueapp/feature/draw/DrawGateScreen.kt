package com.mikhilnaika.continueapp.feature.draw

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikhilnaika.continueapp.core.audio.LocalArcadeAudio
import com.mikhilnaika.continueapp.core.audio.MusicCue
import com.mikhilnaika.continueapp.core.audio.MusicTrack
import com.mikhilnaika.continueapp.core.audio.Sfx
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.ArcadeButton

/**
 * The CONTINUE? screen — docs/02-PRODUCT-SPEC.md §3 "The economy of DRAW". Shown when a free
 * user's second draw of the day is blocked. The countdown loops rather than locking anyone
 * out — atmosphere, not punishment.
 */
@Composable
fun DrawGateScreen(
    state: DrawUiState,
    onInsertCoin: () -> Unit,
    onFreePlay: () -> Unit,
    onUseCoin: () -> Unit,
    onGoPro: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The arcade continue screen always had music: the tune is what made ten seconds feel like
    // a decision. It stops by itself while the rewarded ad covers the app (ON_STOP).
    MusicCue(MusicTrack.CONTINUE)
    val audio = LocalArcadeAudio.current
    LaunchedEffect(state.gateError) { if (state.gateError != null) audio.play(Sfx.ERROR) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ContinueColors.SurfaceVoid)
            .padding(ContinueSpacing.XL.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "CONTINUE?",
            style = ContinueTextStyles.displayL,
            color = ContinueColors.AccentHot,
            textAlign = TextAlign.Center,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.LG.dp))
        Text(
            text = state.gateCountdown.toString(),
            style = com.mikhilnaika.continueapp.core.design.ContinueTextStyles.displayXl,
            color = ContinueColors.AccentCoin,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))
        Text(
            text = "You've used today's free draw.",
            style = ContinueTextStyles.body,
            color = ContinueColors.TextSecondary,
            textAlign = TextAlign.Center,
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XXL.dp))

        if (state.gateBusy) {
            CircularProgressIndicator(color = ContinueColors.AccentCoin)
            state.gateStatus?.let { status ->
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.MD.dp))
                Text(text = status, style = ContinueTextStyles.label, color = ContinueColors.TextSecondary)
            }
        } else {
            GateAction(
                label = "▸ INSERT COIN",
                sublabel = "Watch an ad for +1 coin",
                accent = ContinueColors.AccentNeon,
                onClick = onInsertCoin,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))
            GateAction(
                label = "▸ USE A COIN (${state.coinBalance})",
                sublabel = "Spend a coin to continue",
                accent = ContinueColors.AccentCoin,
                // The one moment in the app that is literally inserting a coin.
                onClick = {
                    audio.play(Sfx.COIN)
                    onUseCoin()
                },
                enabled = state.coinBalance > 0,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))
            // The ad that hands over the most app. Priced in plain words before anything plays —
            // every ad here is a trade the user chooses, never an interruption.
            GateAction(
                label = "▸ FREE PLAY — 60 MIN OF PRO",
                sublabel = "Watch one ad. Everything unlocks for an hour.",
                accent = ContinueColors.AccentHot,
                onClick = onFreePlay,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))
            GateAction(
                label = "▸ GO PRO — UNLIMITED",
                sublabel = "Never see this screen again",
                accent = ContinueColors.AccentCool,
                onClick = onGoPro,
            )
        }

        state.gateError?.let { error ->
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.LG.dp))
            Text(
                text = error,
                style = ContinueTextStyles.label,
                color = ContinueColors.AccentHot,
                textAlign = TextAlign.Center,
            )
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
        ArcadeButton(text = "NOT NOW", onClick = onDismiss, accent = ContinueColors.OutlineDim)
    }
}

@Composable
private fun GateAction(
    label: String,
    sublabel: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ContinueColors.SurfaceRaised)
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = ContinueSpacing.LG.dp, vertical = ContinueSpacing.MD.dp),
    ) {
        Text(
            text = label,
            style = ContinueTextStyles.titleM,
            color = if (enabled) accent else ContinueColors.TextTertiary,
        )
        Text(
            text = sublabel,
            style = ContinueTextStyles.label,
            color = ContinueColors.TextSecondary,
        )
    }
}
