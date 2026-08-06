package com.mikhilnaika.continueapp.feature.completion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.ArcadeButton
import com.mikhilnaika.continueapp.core.util.Haptics
import kotlinx.coroutines.delay

private const val STEP_FLASH = 0
private const val STEP_TITLE = 1
private const val STEP_CREDITS = 2
private const val STEP_COINS = 3
private const val STEP_RANK_BUTTON = 4

/**
 * The completion ritual — docs/02-PRODUCT-SPEC.md §4 "Credits Roll". Not a checkbox: a
 * ~7-second, skippable cinematic. "Completion should pay" — see [CreditsRollViewModel] for
 * the +5 coin grant.
 */
@Composable
fun CreditsRollScreen(
    onRankIt: (Long) -> Unit,
    onSkipToPile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreditsRollViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val haptics = remember { Haptics(context) }
    var step by remember { mutableIntStateOf(STEP_FLASH) }
    var skipped by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        step = STEP_FLASH
        delay(400)
        if (skipped) return@LaunchedEffect
        step = STEP_TITLE
        delay(1200)
        if (skipped) return@LaunchedEffect
        step = STEP_CREDITS
        delay(2600)
        if (skipped) return@LaunchedEffect
        step = STEP_COINS
        haptics.celebratory()
        delay(900)
        if (skipped) return@LaunchedEffect
        step = STEP_RANK_BUTTON
    }

    val flashAlpha by animateFloatAsState(
        targetValue = if (step == STEP_FLASH) 1f else 0f,
        animationSpec = tween(350),
        label = "crtFlash",
    )
    val keyArtAlpha by animateFloatAsState(
        targetValue = if (step >= STEP_TITLE) 0.25f else 0f,
        animationSpec = tween(1500),
        label = "keyArtFade",
    )
    val keyArtScale by animateFloatAsState(
        targetValue = if (step >= STEP_TITLE) 1.15f else 1f,
        animationSpec = tween(7000),
        label = "keyArtZoom",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    skipped = true
                    step = STEP_RANK_BUTTON
                })
            },
    ) {
        if (state.backgroundUrl != null && keyArtAlpha > 0f) {
            AsyncImage(
                model = state.backgroundUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(keyArtAlpha)
                    .graphicsLayer { scaleX = keyArtScale; scaleY = keyArtScale },
                contentScale = ContentScale.Crop,
            )
        }

        if (!state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ContinueSpacing.XL.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (step >= STEP_TITLE) {
                    TypewriterTitle(fullText = "GAME CLEARED", active = step == STEP_TITLE)
                }
                if (step >= STEP_CREDITS) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XXL.dp))
                    CreditsBlock(state = state)
                }
                if (step >= STEP_COINS) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.LG.dp))
                    Text(
                        text = "+5 COINS",
                        style = ContinueTextStyles.monoL,
                        color = ContinueColors.AccentCoin,
                    )
                }
                if (step >= STEP_RANK_BUTTON) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
                    ArcadeButton(text = "RANK IT ▸", onClick = { onRankIt(state.gameId) })
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))
                    Text(
                        text = "SKIP",
                        style = ContinueTextStyles.label,
                        color = ContinueColors.TextTertiary,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(onTap = { onSkipToPile() })
                        },
                    )
                }
            }
        }

        if (flashAlpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(ContinueColors.TextPrimary.copy(alpha = flashAlpha * 0.9f)))
        }
    }
}

@Composable
private fun TypewriterTitle(fullText: String, active: Boolean) {
    var shown by remember(fullText) { mutableIntStateOf(if (active) 0 else fullText.length) }
    LaunchedEffect(active) {
        if (!active) {
            shown = fullText.length
            return@LaunchedEffect
        }
        for (i in 1..fullText.length) {
            shown = i
            delay(60)
        }
    }
    Text(
        text = fullText.take(shown),
        style = ContinueTextStyles.displayXl,
        color = ContinueColors.AccentNeon,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CreditsBlock(state: CreditsRollUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CreditLine("CLEARED BY", "YOU")
        CreditLine("TIME IN THE PILE", "${state.daysInThePile} DAYS")
        CreditLine("STARTED", state.startedLabel)
        CreditLine("FINISHED", state.finishedLabel)
        CreditLine("YOUR ${ordinalSuffix(state.clearOrdinal)} CLEAR", "OF ${state.clearYear}")
    }
}

@Composable
private fun CreditLine(label: String, value: String) {
    Row {
        Text(text = label, style = ContinueTextStyles.label, color = ContinueColors.TextSecondary, modifier = Modifier.padding(end = 8.dp))
        Text(text = value, style = ContinueTextStyles.label, color = ContinueColors.TextPrimary)
    }
}

@Composable
private fun Row(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(vertical = 2.dp),
        content = content,
    )
}

private fun ordinalSuffix(n: Int): String {
    val suffix = if (n % 100 in 11..13) "TH" else when (n % 10) {
        1 -> "ST"; 2 -> "ND"; 3 -> "RD"; else -> "TH"
    }
    return "$n$suffix"
}
