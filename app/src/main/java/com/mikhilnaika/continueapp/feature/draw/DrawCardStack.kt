package com.mikhilnaika.continueapp.feature.draw

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueMotion
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.util.Haptics
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
            }
            DrawPhase.DONE -> DrawDoneSummary(state = state, onDone = onDone)
            else -> Unit
        }
    }
}

@Composable
private fun DealingIndicator(count: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        DealingCardsRow(count)
    }
}

@Composable
private fun DealingCardsRow(count: Int) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
        repeat(count) { index ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(index * 90L)
                visible = true
            }
            val offsetY by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (visible) 0f else 120f,
                animationSpec = ContinueMotion.card(),
                label = "dealCard$index",
            )
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = offsetY }
                    .size(width = 70.dp, height = 96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ContinueColors.SurfaceRaised),
            )
        }
    }
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
                            model = coverUrl,
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
