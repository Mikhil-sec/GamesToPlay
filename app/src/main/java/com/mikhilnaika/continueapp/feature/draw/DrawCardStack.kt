package com.mikhilnaika.continueapp.feature.draw

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueMotion
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.IgdbAttribution
import com.mikhilnaika.continueapp.core.util.Haptics
import com.mikhilnaika.continueapp.core.util.IgdbImage
import kotlin.math.abs
import kotlinx.coroutines.launch

private const val COMMIT_DISTANCE_PX = 260f
private const val COMMIT_VELOCITY = 900f

/**
 * The card deal + flip + swipe interaction — docs/02-PRODUCT-SPEC.md §3. Cards deal with a
 * stagger, flip face-up, and each swipe direction carries a distinct verdict. Velocity, not
 * just distance, decides the commit so a fast flick past the deadzone still counts.
 */
@Composable
fun DrawCardStack(
    state: DrawUiState,
    onVerdict: (SwipeVerdict) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    haptics: Haptics? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ContinueColors.SurfaceFelt)
            .padding(ContinueSpacing.LG.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state.phase) {
            DrawPhase.DEALING -> DealingIndicator(count = state.picks.size.coerceAtLeast(3))
            DrawPhase.CARDS -> {
                Text(
                    text = "${state.currentCardIndex + 1} / ${state.picks.size}",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextSecondary,
                )
                state.loosenedMessage?.let {
                    Text(
                        text = it.uppercase(),
                        style = ContinueTextStyles.label,
                        color = ContinueColors.AccentCoin,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    state.currentPick?.let { pick ->
                        SwipeableCard(
                            key = pick.candidate.entryId,
                            title = pick.candidate.name,
                            coverUrl = pick.candidate.coverUrl,
                            reasons = pick.reasons,
                            onVerdict = onVerdict,
                            haptics = haptics,
                        )
                    }
                }
                SwipeLegend()
                // The dealt cards are IGDB covers and IGDB titles, so the credit belongs on
                // this phase — the DIALS screen before it shows none of their data, and its
                // layout is measured to the pixel around the lever anyway.
                IgdbAttribution()
            }
            DrawPhase.DONE -> DrawDoneSummary(state = state, onDone = onDone)
            else -> Unit
        }
    }
}

/** How long each card takes to clear the slot before the next one is pushed out. */
internal const val DEAL_STAGGER_MS = 190L

/**
 * How long the DEALING phase must stay on screen for the whole dispense to land.
 *
 * Shared with [DrawViewModel] so the two can't drift: the phase used to end on a hardcoded
 * 900ms, which would now cut the last card off mid-eject. Three cards are always dealt during
 * DEALING (the real picks aren't known yet), the last one starts at `2 * stagger`, and the
 * spring needs roughly another 600ms to settle.
 */
internal const val DEAL_ANIMATION_MS = 2 * DEAL_STAGGER_MS + 600L

/**
 * The machine dispensing your hand — docs/02-PRODUCT-SPEC.md §3.
 *
 * This used to be three grey rectangles sliding up, which read as a loading spinner rather than
 * as the cabinet doing something. Now the cards are *ejected one at a time from a slot*: each
 * one starts inside the machine (squashed flat, behind the lip), shoots up past its resting
 * place, and settles with a spring, while the slot flashes as it passes through.
 *
 * Deliberately built from transforms only — no bitmaps, no reel rig. A full slot-machine reel
 * animation is the kind of thing that either has real art behind it or looks cheap, and there's
 * no art budget here; a dispenser reads as mechanical *and* survives being drawn in code.
 * Every value below is scaled off the container so it holds up on a phone and a tablet alike.
 */
@Composable
private fun DealingIndicator(count: Int) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val cardWidth = (maxWidth / (count + 1).coerceAtLeast(3)).coerceAtMost(96.dp)
        val cardHeight = cardWidth * 1.4f
        // Where the slot sits relative to the fanned cards — everything animates out of here.
        val slotDropPx = with(LocalDensity.current) { (cardHeight + 40.dp).toPx() }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                repeat(count) { index ->
                    EjectedCard(
                        index = index,
                        total = count,
                        width = cardWidth,
                        height = cardHeight,
                        slotDropPx = slotDropPx,
                    )
                }
            }
            Spacer(modifier = Modifier.padding(top = 20.dp))
            DispenserSlot(width = cardWidth * count + 48.dp, cardCount = count)
        }
    }
}

@Composable
private fun EjectedCard(index: Int, total: Int, width: Dp, height: Dp, slotDropPx: Float) {
    var ejected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * DEAL_STAGGER_MS)
        ejected = true
    }

    // Springy rather than tweened: a card leaving a machine has momentum, and the small
    // overshoot is what sells it as thrown instead of faded in.
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (ejected) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.62f,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
        ),
        label = "eject$index",
    )

    // Fan the settled cards out from the centre so the hand doesn't read as a flat row.
    val fanDegrees = (index - (total - 1) / 2f) * 7f

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = slotDropPx * (1f - progress)
                // Squashed while still inside the slot, full height once clear of it.
                scaleY = 0.35f + 0.65f * progress
                scaleX = 0.9f + 0.1f * progress
                rotationZ = fanDegrees * progress
                alpha = progress.coerceIn(0f, 1f)
            }
            .size(width = width, height = height)
            .clip(RoundedCornerShape(8.dp))
            .background(ContinueColors.SurfaceRaised),
    ) {
        // A thin lit edge so the face-down backs aren't flat blocks in the dark.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(ContinueColors.AccentCoin.copy(alpha = 0.35f * progress)),
        )
    }
}

/** The lip the cards come out of. Pulses coin-yellow once per card as it passes through. */
@Composable
private fun DispenserSlot(width: Dp, cardCount: Int) {
    var pulses by remember { mutableStateOf(0) }
    LaunchedEffect(cardCount) {
        repeat(cardCount) {
            kotlinx.coroutines.delay(DEAL_STAGGER_MS)
            pulses++
        }
    }
    val glow by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pulses > 0) 0.18f else 0.05f,
        animationSpec = tween(140),
        label = "slotGlow",
    )
    Box(
        modifier = Modifier
            .size(width = width, height = 10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(ContinueColors.AccentCoin.copy(alpha = glow)),
    )
}

@Composable
private fun SwipeableCard(
    key: Long,
    title: String,
    coverUrl: String?,
    reasons: List<String>,
    onVerdict: (SwipeVerdict) -> Unit,
    haptics: Haptics?,
) {
    val offsetX = remember(key) { Animatable(0f) }
    val offsetY = remember(key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var flipped by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        kotlinx.coroutines.delay(60)
        flipped = true
    }
    val rotationY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (flipped) 0f else 180f,
        animationSpec = tween(ContinueMotion.EMPHASIZED_DURATION_MS),
        label = "cardFlip",
    )

    fun animateOffsetTo(target: Offset, spec: androidx.compose.animation.core.AnimationSpec<Float>) {
        scope.launch { offsetX.animateTo(target.x, spec) }
        scope.launch { offsetY.animateTo(target.y, spec) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .aspectRatio(3f / 4.2f)
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = (offsetX.value / 32f).coerceIn(-18f, 18f)
                this.rotationY = rotationY
                cameraDistance = 12f * density
            }
            .pointerInput(key) {
                val tracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = { tracker.resetTracking() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                        scope.launch { offsetY.snapTo(offsetY.value + dragAmount.y) }
                    },
                    onDragEnd = {
                        val velocity = tracker.calculateVelocity()
                        val verdict = resolveVerdict(Offset(offsetX.value, offsetY.value), velocity)
                        if (verdict != null) {
                            haptics?.heavy()
                            val target = exitTarget(verdict)
                            animateOffsetTo(target, tween(220))
                            scope.launch {
                                kotlinx.coroutines.delay(220)
                                onVerdict(verdict)
                            }
                        } else {
                            animateOffsetTo(Offset.Zero, ContinueMotion.card())
                        }
                    },
                )
            },
    ) {
        if (rotationY > 90f) {
            // Back of the card — still face-down.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ContinueColors.SurfaceRaised),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ContinueColors.SurfaceCabinet),
            ) {
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.72f).background(ContinueColors.SurfaceRaised)) {
                    if (coverUrl != null) {
                        AsyncImage(
                            // This is the biggest a cover ever gets drawn in the app — the
                            // baseline 264px token was being upscaled ~3x here.
                            model = IgdbImage.at(coverUrl, IgdbImage.HERO),
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = title.take(1).uppercase(),
                            style = ContinueTextStyles.displayXl,
                            color = ContinueColors.TextTertiary,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
                Column(modifier = Modifier.padding(ContinueSpacing.MD.dp)) {
                    Text(text = title, style = ContinueTextStyles.titleL, color = ContinueColors.TextPrimary, maxLines = 1)
                    Text(
                        text = reasons.joinToString(" · "),
                        style = ContinueTextStyles.label,
                        color = ContinueColors.AccentNeon,
                    )
                }
            }
        }
    }
}

private fun resolveVerdict(offset: Offset, velocity: androidx.compose.ui.unit.Velocity): SwipeVerdict? {
    val dx = offset.x
    val dy = offset.y
    val vx = velocity.x
    val vy = velocity.y
    val committedX = abs(dx) > COMMIT_DISTANCE_PX || abs(vx) > COMMIT_VELOCITY
    val committedY = abs(dy) > COMMIT_DISTANCE_PX || abs(vy) > COMMIT_VELOCITY
    if (!committedX && !committedY) return null
    // Whichever axis moved further decides the verdict.
    return if (abs(dx) + abs(vx) / 4f > abs(dy) + abs(vy) / 4f) {
        if (dx > 0) SwipeVerdict.SAVE_FOR_LATER else SwipeVerdict.NOT_TONIGHT
    } else {
        if (dy < 0) SwipeVerdict.PLAYING_IT else SwipeVerdict.RETIRE
    }
}

private fun exitTarget(verdict: SwipeVerdict): Offset = when (verdict) {
    SwipeVerdict.PLAYING_IT -> Offset(0f, -1600f)
    SwipeVerdict.NOT_TONIGHT -> Offset(-1600f, 0f)
    SwipeVerdict.SAVE_FOR_LATER -> Offset(1600f, 0f)
    SwipeVerdict.RETIRE -> Offset(0f, 1600f)
}

@Composable
private fun SwipeLegend() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = ContinueSpacing.MD.dp)) {
        Text(
            text = "↑ PLAYING IT     ↓ RETIRE IT",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextSecondary,
        )
        Text(
            text = "← NOT TONIGHT     SAVE FOR LATER →",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextTertiary,
        )
    }
}

@Composable
private fun DrawDoneSummary(state: DrawUiState, onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "THAT'S THE DEAL", style = ContinueTextStyles.displayL, color = ContinueColors.TextPrimary)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.LG.dp))
            com.mikhilnaika.continueapp.core.ui.ArcadeButton(text = "BACK TO THE PILE", onClick = onDone)
        }
    }
}
