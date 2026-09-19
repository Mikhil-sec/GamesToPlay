package com.mikhilnaika.continueapp.feature.draw

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikhilnaika.continueapp.core.audio.LocalArcadeAudio
import com.mikhilnaika.continueapp.core.audio.Sfx
import com.mikhilnaika.continueapp.core.data.Mood
import com.mikhilnaika.continueapp.core.data.TimeBudget
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueMotion
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.EmptyState
import com.mikhilnaika.continueapp.core.util.GameFacet
import com.mikhilnaika.continueapp.core.ui.LocalHaptics
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
    onGoPro: () -> Unit = {},
    viewModel: DrawViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHaptics.current

    // The paywall is a separate destination, so DRAW is recomposed on the way back with a
    // possibly-changed entitlement. If PRO was bought there, drop the gate and deal.
    androidx.compose.runtime.LaunchedEffect(state.isPro) { viewModel.onReturnedFromPaywall() }

    when (state.phase) {
        DrawPhase.DIALS -> DrawDialsScreen(
            state = state,
            onTimeBudget = viewModel::setTimeBudget,
            onMood = viewModel::setMood,
            onTogglePlatform = viewModel::togglePlatform,
            onToggleFacet = viewModel::toggleFacet,
            onPullLever = viewModel::pullLever,
            haptics = haptics,
            modifier = modifier,
        )
        DrawPhase.GATE -> DrawGateScreen(
            state = state,
            onInsertCoin = { context.findActivity()?.let(viewModel::insertCoin) },
            onFreePlay = { context.findActivity()?.let(viewModel::freePlay) },
            onUseCoin = viewModel::useCoin,
            onGoPro = onGoPro,
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

/**
 * A phone can't show the dials *and* the lever at once — the three dial sections wrap to eight
 * or more rows of chips once a pile has a realistic set of platforms, which on a 6" screen left
 * the lever squeezed to a sliver that couldn't be dragged at all. So the dials collapse (with
 * their current setting still spelled out, the same rule as PILE's filters), the dials that are
 * shown scroll in their own region, and **the lever always keeps its full pull height** — it is
 * the one thing on this screen that must never be compressed, because it's a gesture, not a
 * button.
 */
@Composable
private fun DrawDialsScreen(
    state: DrawUiState,
    onTimeBudget: (TimeBudget) -> Unit,
    onMood: (Mood) -> Unit,
    onTogglePlatform: (String) -> Unit,
    onToggleFacet: (GameFacet) -> Unit,
    onPullLever: () -> Unit,
    haptics: Haptics,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ContinueColors.SurfaceCabinet),
    ) {
        // Height, not width, is what decides this: a tablet can afford every dial open at once,
        // a phone can't. Measuring beats a hardcoded breakpoint because it also covers landscape.
        val compact = maxHeight < 720.dp
        val leverTrackHeight = if (maxHeight < 560.dp) 150.dp else LEVER_TRACK_HEIGHT
        var dialsExpanded by rememberSaveable { mutableStateOf(!compact) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ContinueSpacing.LG.dp, vertical = ContinueSpacing.MD.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "THE MACHINE",
                    style = if (compact) ContinueTextStyles.titleL else ContinueTextStyles.displayL,
                    color = ContinueColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = dialsExpanded,
                    onClick = { dialsExpanded = !dialsExpanded },
                    label = { Text(if (dialsExpanded) "HIDE DIALS" else "DIALS") },
                    trailingIcon = {
                        Icon(
                            imageVector = if (dialsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (dialsExpanded) "Hide the dials" else "Show the dials",
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }

            // Collapsing the dials must never hide *what they're set to* — otherwise a draw
            // comes back filtered by settings the user can't see.
            if (!dialsExpanded) {
                Text(
                    text = dialSummary(state),
                    style = ContinueTextStyles.label,
                    color = ContinueColors.AccentCoin,
                    modifier = Modifier.padding(top = ContinueSpacing.SM.dp),
                )
            }

            if (dialsExpanded) {
                // The lever below carries no weight, so it is measured *first* and keeps its full
                // height; this region only ever gets what's genuinely left over, and scrolls
                // whatever doesn't fit rather than squeezing the lever out of existence.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
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
                    // GENRE sits between MOOD and PLATFORM because that is the order the
                    // question gets asked: how long have I got, what am I in the mood for, what
                    // *kind* of thing, and only then what am I willing to boot up.
                    if (state.availableFacets.isNotEmpty()) {
                        DialSection(title = "GENRE") {
                            state.availableFacets.forEach { facet ->
                                FilterChip(
                                    selected = facet in state.selectedFacets,
                                    onClick = { onToggleFacet(facet) },
                                    label = { Text(facet.label) },
                                )
                            }
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
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = ContinueSpacing.SM.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DrawLever(onPull = onPullLever, haptics = haptics, trackHeight = leverTrackHeight)
                }
            } else {
                // Dials hidden: the lever gets the whole cabinet and sits centred in it, which is
                // the screen the machine deserves anyway — one thing to do, in the middle.
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    DrawLever(onPull = onPullLever, haptics = haptics, trackHeight = leverTrackHeight)
                }
            }
        }
    }
}

/** What the machine is currently set to, in one line, for when the dials are hidden. */
private fun dialSummary(state: DrawUiState): String = listOfNotNull(
    budgetLabel(state.timeBudget),
    state.mood.name,
    when (state.selectedFacets.size) {
        0 -> null
        1 -> state.selectedFacets.first().label
        else -> "${state.selectedFacets.size} GENRES"
    },
    when (state.selectedPlatforms.size) {
        0 -> null
        1 -> state.selectedPlatforms.first().uppercase()
        else -> "${state.selectedPlatforms.size} PLATFORMS"
    },
).joinToString("  \u00b7  ")

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

private val LEVER_TRACK_HEIGHT = 220.dp
private val LEVER_KNOB_SIZE = 56.dp

/** How far down the track counts as a real pull. Proportional, so a shorter track still works. */
private const val LEVER_PULL_THRESHOLD_FRACTION = 0.68f

/**
 * A physical lever, not a button — docs/02-PRODUCT-SPEC.md §3: "The difference is the whole
 * point." Drags down with spring resistance and snaps back up after releasing past threshold,
 * firing [onPull] with a heavy haptic.
 */
@Composable
private fun DrawLever(onPull: () -> Unit, haptics: Haptics, trackHeight: Dp = LEVER_TRACK_HEIGHT) {
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val audio = LocalArcadeAudio.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val trackPx = with(density) { trackHeight.toPx() }
    val knobPx = with(density) { LEVER_KNOB_SIZE.toPx() }
    val thresholdPx = (trackPx - knobPx) * LEVER_PULL_THRESHOLD_FRACTION

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(trackHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(ContinueColors.SurfaceRaised),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { translationY = offsetY.value }
                    .size(LEVER_KNOB_SIZE)
                    .clip(CircleShape)
                    .background(ContinueColors.AccentHot)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                val pulled = offsetY.value >= thresholdPx
                                scope.launch { offsetY.animateTo(0f, ContinueMotion.heavy()) }
                                if (pulled) {
                                    haptics.heavy()
                                    audio.play(Sfx.LEVER)
                                    onPull()
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    val next = (offsetY.value + dragAmount)
                                        .coerceIn(0f, trackPx - knobPx)
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
