package com.mikhilnaika.continueapp.feature.draw

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikhilnaika.continueapp.core.data.Mood
import com.mikhilnaika.continueapp.core.data.TimeBudget
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueMotion
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.EmptyState
import com.mikhilnaika.continueapp.core.util.Haptics
import com.mikhilnaika.continueapp.core.util.findActivity
import kotlinx.coroutines.launch

/**
 * The DRAW machine — docs/02-PRODUCT-SPEC.md §3, the signature feature. Routes between the
 * dial-setting cabinet, the CONTINUE? economy gate, and the card deal/flip/swipe interaction
 * by [DrawUiState.phase].
 */
@Composable
fun DrawScreen(
    modifier: Modifier = Modifier,
    onNavigateToDiscover: () -> Unit = {},
    viewModel: DrawViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val haptics = remember { Haptics(context) }

    when (state.phase) {
        DrawPhase.DIALS -> DrawDialsScreen(
            state = state,
            onTimeBudget = viewModel::setTimeBudget,
            onMood = viewModel::setMood,
            onTogglePlatform = viewModel::togglePlatform,
            onPullLever = viewModel::pullLever,
            haptics = haptics,
            modifier = modifier,
        )
        DrawPhase.GATE -> DrawGateScreen(
            state = state,
            onInsertCoin = { context.findActivity()?.let(viewModel::insertCoin) },
            onUseCoin = viewModel::useCoin,
            onGoPro = { context.findActivity()?.let(viewModel::goPro) },
            onDismiss = viewModel::dismissGate,
            modifier = modifier,
        )
        DrawPhase.DEALING, DrawPhase.CARDS, DrawPhase.DONE -> DrawCardStack(
            state = state,
            onVerdict = { verdict ->
                if (verdict == SwipeVerdict.PLAYING_IT) haptics.celebratory()
                viewModel.applyVerdict(verdict)
            },
            onDone = viewModel::playAgain,
            haptics = haptics,
            modifier = modifier,
        )
        DrawPhase.EMPTY_PILE -> EmptyState(
            headline = "NOTHING IN THE PILE TO DRAW FROM",
            modifier = modifier.fillMaxSize(),
            action = {
                com.mikhilnaika.continueapp.core.ui.ArcadeButton(text = "FIND SOMETHING TO PLAY", onClick = onNavigateToDiscover)
            },
        )
    }
}

@Composable
private fun DrawDialsScreen(
    state: DrawUiState,
    onTimeBudget: (TimeBudget) -> Unit,
    onMood: (Mood) -> Unit,
    onTogglePlatform: (String) -> Unit,
    onPullLever: () -> Unit,
    haptics: Haptics,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ContinueColors.SurfaceCabinet)
            .padding(ContinueSpacing.LG.dp),
    ) {
        Text(text = "THE MACHINE", style = ContinueTextStyles.displayL, color = ContinueColors.TextPrimary)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))

        DialSection(title = "TIME") {
            TimeBudget.entries.forEach { budget ->
                FilterChip(
                    selected = state.timeBudget == budget,
                    onClick = { onTimeBudget(budget) },
                    label = { Text(budgetLabel(budget)) },
                )
            }
        }
        DialSection(title = "MOOD") {
            Mood.entries.forEach { mood ->
                FilterChip(
                    selected = state.mood == mood,
                    onClick = { onMood(mood) },
                    label = { Text(mood.name) },
                )
            }
        }
        if (state.availablePlatforms.isNotEmpty()) {
            DialSection(title = "PLATFORM") {
                state.availablePlatforms.forEach { platform ->
                    FilterChip(
                        selected = platform in state.selectedPlatforms,
                        onClick = { onTogglePlatform(platform) },
                        label = { Text(platform.uppercase()) },
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            DrawLever(onPull = onPullLever, haptics = haptics)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DialSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = ContinueSpacing.SM.dp)) {
        Text(text = title, style = ContinueTextStyles.label, color = ContinueColors.TextSecondary)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) { content() }
    }
}

private fun budgetLabel(budget: TimeBudget): String = when (budget) {
    TimeBudget.THIRTY_MIN -> "30 MIN"
    TimeBudget.TWO_HOURS -> "2 HOURS"
    TimeBudget.ALL_NIGHT -> "ALL NIGHT"
    TimeBudget.A_WHOLE_WEEKEND -> "A WHOLE WEEKEND"
}

private const val LEVER_TRACK_HEIGHT_DP = 220
private const val LEVER_PULL_THRESHOLD_DP = 150

/**
 * A physical lever, not a button — docs/02-PRODUCT-SPEC.md §3: "The difference is the whole
 * point." Drags down with spring resistance and snaps back up after releasing past threshold,
 * firing [onPull] with a heavy haptic.
 */
@Composable
private fun DrawLever(onPull: () -> Unit, haptics: Haptics) {
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val trackPx = with(density) { LEVER_TRACK_HEIGHT_DP.dp.toPx() }
    val thresholdPx = with(density) { LEVER_PULL_THRESHOLD_DP.dp.toPx() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(LEVER_TRACK_HEIGHT_DP.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ContinueColors.SurfaceRaised),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { translationY = offsetY.value }
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ContinueColors.AccentHot)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                val pulled = offsetY.value >= thresholdPx
                                scope.launch { offsetY.animateTo(0f, ContinueMotion.heavy()) }
                                if (pulled) {
                                    haptics.heavy()
                                    onPull()
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    val next = (offsetY.value + dragAmount)
                                        .coerceIn(0f, trackPx - with(density) { 56.dp.toPx() })
                                    offsetY.snapTo(next)
                                }
                            },
                        )
                    },
            )
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))
        Text(text = "PULL TO DRAW", style = ContinueTextStyles.label, color = ContinueColors.TextSecondary)
    }
}
