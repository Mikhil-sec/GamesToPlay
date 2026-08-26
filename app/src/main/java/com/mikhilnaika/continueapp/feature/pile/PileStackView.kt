package com.mikhilnaika.continueapp.feature.pile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueMotion
import com.mikhilnaika.continueapp.core.design.ContinueShapes
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.util.Haptics
import com.mikhilnaika.continueapp.core.util.IgdbImage
import com.mikhilnaika.continueapp.core.util.playtimeLabel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How many cards stay drawn behind the front one. Six deep is enough to read as "a pile" —
 * past that the cards are small enough and faded enough that nobody counts them, and each one
 * is another cover bitmap held in memory.
 */
private const val VISIBLE_BEHIND = 5

/** Per card of depth, as a fraction of card height / a scale factor / an alpha. */
private const val DEPTH_STEP_FRACTION = 0.115f
private const val DEPTH_SCALE_LOSS = 0.075f
private const val DEPTH_ALPHA_LOSS = 0.155f

/**
 * Perspective tilt per card of depth, and its cap. Deliberately small: a heavy tilt is the
 * fastest way to make a 2D transform read as a broken layout rather than as depth, and this is
 * the one value here that can only really be judged on glass.
 */
private const val DEPTH_TILT_DEGREES = 4f
private const val MAX_TILT_DEGREES = 12f

/**
 * A card that's been flicked past travels up *past the camera* rather than joining the recede:
 * it grows, tilts, and is gone before it has covered half its travel. That fast fade is what
 * keeps it from colliding visually with the shrinking cards behind it, which occupy the same
 * strip of screen.
 */
private const val EXIT_TRAVEL_FRACTION = 1.35f
private const val EXIT_SCALE_GAIN = 0.28f
private const val EXIT_FADE_RATE = 2.2f

/** Drag distance that advances the stack by exactly one card, as a fraction of card height. */
private const val DRAG_UNIT_FRACTION = 0.45f

/**
 * Cap on how far one fling can travel. Momentum is the point of the view, but an uncapped
 * decay across an 87-game pile lands somewhere nobody aimed for and with nothing decoded yet.
 */
private const val MAX_FLING_CARDS = 8

/** Travel of the hint's breathing chevrons, in dp. Small — it has to read as a nudge. */
private const val HINT_DRIFT_DP = 6f

/**
 * The card index a continuous scroll [position] is currently pointing at, guaranteed to be a
 * valid index into a list whose last index is [lastIndex].
 *
 * Pulled out as a pure function precisely because *not* clamping it against the current list was
 * the STACK crash: [position] is an `Animatable` that outlives the list under it, so every read
 * of it as an index has to be re-clamped at the point of use rather than once, early, against
 * whatever the list happened to be then.
 */
internal fun stackAnchor(position: Float, lastIndex: Int): Int =
    if (lastIndex <= 0) 0 else position.roundToInt().coerceIn(0, lastIndex)

/**
 * STACK — the signature view of PILE (docs/02-PRODUCT-SPEC.md §1 "Views"): the pile as physical
 * cases receding into the screen, flicked through with momentum and snapped to a card.
 *
 * Built by hand rather than on a `Pager` because a pager lays its pages out end to end and this
 * view is defined by pages *overlapping* — every card is drawn in the same place and pushed
 * apart purely by transform. It follows the same idiom as `DrawCardStack`: an `Animatable`
 * holding a fractional card index, a `VelocityTracker` for the fling, and springs rather than
 * curves.
 *
 * Everything is sized off `BoxWithConstraints`, never off a device class, so a phone, a tablet
 * and a shrunken-by-expanded-filters stack are all the same code path.
 */
@Composable
fun PileStackView(
    entries: List<PileEntryWithGame>,
    onSelect: (PileEntryWithGame) -> Unit,
    modifier: Modifier = Modifier,
    /** Changing this rewinds the stack to the top — a new tab or a new filter is a new pile. */
    resetKey: Any? = null,
    haptics: Haptics? = null,
    /** One-time teach for the drag gesture — see [SwipeHint]. */
    showHint: Boolean = false,
    onHintDismissed: () -> Unit = {},
) {
    if (entries.isEmpty()) return
    val lastIndex = entries.lastIndex

    val position = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val decay = rememberSplineBasedDecay<Float>()

    // The gesture handlers below are keyed on size, not contents, so they'd otherwise capture a
    // stale list every time an entry is edited in place.
    val currentEntries by rememberUpdatedState(entries)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnHintDismissed by rememberUpdatedState(onHintDismissed)

    LaunchedEffect(resetKey) { position.snapTo(0f) }

    // Moving a game out of the current tab shortens the list under the stack. Clamping (rather
    // than rewinding) keeps you where you were looking, which is the whole reason the reset
    // above is keyed on the tab and not on the contents.
    LaunchedEffect(lastIndex) {
        if (position.value > lastIndex) position.snapTo(lastIndex.toFloat())
    }

    // One detent tick per card passed, drag and fling alike — the decelerating burst at the end
    // of a flick is the reel-stopping feel the cabinet is going for. `drop(1)` so arriving on
    // the screen isn't itself a buzz.
    LaunchedEffect(haptics) {
        snapshotFlow { position.value.roundToInt() }
            .distinctUntilChanged()
            .drop(1)
            .collect { haptics?.light() }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Height-led: the stack needs headroom above the front card for the recede and room
        // below it for the caption, and height is the scarce axis on a phone. The width clamp
        // is what stops a short landscape window from producing a card wider than the screen.
        val cardHeight = (maxHeight * 0.60f).coerceAtMost(maxWidth * 1.05f)
        val cardWidth = cardHeight * 3f / 4f

        val cardHeightPx = with(LocalDensity.current) { cardHeight.toPx() }
        val stepPx = cardHeightPx * DEPTH_STEP_FRACTION
        val exitPx = cardHeightPx * EXIT_TRAVEL_FRACTION
        val dragUnitPx = cardHeightPx * DRAG_UNIT_FRACTION
        // The stack grows upward, so centring the front card would leave the whole thing sitting
        // high with dead space beneath. Push everything down by half the pile's depth instead,
        // which centres its visual mass.
        val liftPx = VISIBLE_BEHIND * stepPx * 0.5f

        // Read through a lambda inside `graphicsLayer` so the per-frame transforms run in the
        // draw phase; only `anchor` (the rounded card index) is allowed to recompose, and it
        // changes a handful of times per fling rather than sixty times a second.
        //
        // Both of these clamp, and both are rebuilt when `lastIndex` changes, because the two
        // effects that pull `position` back into range are `LaunchedEffect`s — they run a frame
        // *after* the composition that swapped the list, so composition has to be able to survive
        // a `position` that belongs to the previous list on its own.
        val positionProvider = { position.value.coerceIn(0f, lastIndex.toFloat()) }
        // `remember(lastIndex)`, not `remember`: the key is the entire crash. Without it the
        // lambda closed over the list length at *first* composition forever. Switching to a
        // shorter tab then left `anchor` past the end of the new list and `entries[anchor]` below
        // threw (Play Console: "Index: 7, Size: 1", "length=2; index=2"), while a list that grew
        // instead froze the cards at the stale ceiling with `position` — and its per-card
        // haptics — still running on underneath: the "buzzing at an empty stack" report.
        val anchor by remember(lastIndex) { derivedStateOf { stackAnchor(position.value, lastIndex) } }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(lastIndex) {
                    // Tapping anywhere acts on the front card: the whole area is one big target,
                    // which matters because the card itself is a moving object.
                    // Clamped against the list the gesture is *reading*, not the one this
                    // `pointerInput` was keyed on — a tap can land in the gap between the list
                    // changing and this block being torn down and re-launched.
                    detectTapGestures {
                        val entries = currentEntries
                        if (entries.isNotEmpty()) {
                            currentOnSelect(entries[stackAnchor(position.value, entries.lastIndex)])
                        }
                    }
                }
                .pointerInput(lastIndex, dragUnitPx) {
                    val tracker = VelocityTracker()
                    detectVerticalDragGestures(
                        onDragStart = {
                            tracker.resetTracking()
                            // The gesture has been discovered — the hint has done its job and
                            // must not sit over the cards the user is now flicking.
                            currentOnHintDismissed()
                        },
                        onDragEnd = {
                            // Velocity in cards per second, positive = further into the pile.
                            val velocity = -tracker.calculateVelocity().y / dragUnitPx
                            scope.launch {
                                val projected = decay.calculateTargetValue(position.value, velocity)
                                val target = projected.roundToInt()
                                    .coerceIn(anchor - MAX_FLING_CARDS, anchor + MAX_FLING_CARDS)
                                    .coerceIn(0, lastIndex)
                                position.animateTo(
                                    targetValue = target.toFloat(),
                                    animationSpec = ContinueMotion.card(),
                                    initialVelocity = velocity,
                                )
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            scope.launch {
                                position.snapTo(
                                    (position.value - dragAmount / dragUnitPx).coerceIn(0f, lastIndex.toFloat())
                                )
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            // Deepest first: later children draw on top, and the card nearest the viewer — the
            // one on its way out — has to be the last one painted.
            val deepest = (anchor + VISIBLE_BEHIND + 1).coerceAtMost(lastIndex)
            val nearest = (anchor - 1).coerceAtLeast(0)
            for (index in deepest downTo nearest) {
                StackCard(
                    entry = entries[index],
                    index = index,
                    positionProvider = positionProvider,
                    width = cardWidth,
                    height = cardHeight,
                    stepPx = stepPx,
                    exitPx = exitPx,
                    liftPx = liftPx,
                )
            }

            StackCaption(
                entry = entries[anchor],
                index = anchor,
                total = entries.size,
                positionProvider = positionProvider,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = ContinueSpacing.LG.dp),
            )

            SwipeHint(
                visible = showHint && entries.size > 1,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = ContinueSpacing.SM.dp),
            )
        }
    }
}

/**
 * The one-time teach for STACK's only gesture.
 *
 * STACK is PILE's default view and nothing about it says it moves: a closed-test tester read
 * the receding cards as a decorative header and never tried to flick them, so their pile
 * looked one game deep. Two breathing chevrons and the word SWIPE, down the right-hand edge
 * where they can't cover a cover.
 *
 * It dismisses on the *first drag*, not on a timer and not on a tap: the only thing it is
 * teaching is the drag, so the drag is the only event that proves it worked. Hidden outright
 * for a one-game pile, where there is nothing to swipe to and the prompt would just be wrong.
 */
@Composable
private fun SwipeHint(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        val drift by rememberInfiniteTransition(label = "swipe-hint").animateFloat(
            initialValue = -HINT_DRIFT_DP,
            targetValue = HINT_DRIFT_DP,
            animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
            label = "drift",
        )
        val density = LocalDensity.current
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { translationY = with(density) { drift.dp.toPx() } },
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = ContinueColors.AccentCoin,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "SWIPE",
                style = ContinueTextStyles.label,
                color = ContinueColors.AccentCoin,
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Swipe up for the next game, down for the previous one",
                tint = ContinueColors.AccentCoin,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * One case in the pile. The whole visual is transform-only — no bitmap, no shadow layer — so it
 * costs a matrix per frame and nothing else.
 */
@Composable
private fun StackCard(
    entry: PileEntryWithGame,
    index: Int,
    positionProvider: () -> Float,
    width: Dp,
    height: Dp,
    stepPx: Float,
    exitPx: Float,
    liftPx: Float,
) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .graphicsLayer {
                val rel = index - positionProvider()
                cameraDistance = 14f * density
                if (rel >= 0f) {
                    translationY = liftPx - rel * stepPx
                    val shrink = (1f - DEPTH_SCALE_LOSS * rel).coerceAtLeast(0.5f)
                    scaleX = shrink
                    scaleY = shrink
                    alpha = (1f - DEPTH_ALPHA_LOSS * rel).coerceIn(0f, 1f)
                    rotationX = -(DEPTH_TILT_DEGREES * rel).coerceAtMost(MAX_TILT_DEGREES)
                } else {
                    val travelled = -rel
                    translationY = liftPx - travelled * exitPx
                    val grow = 1f + EXIT_SCALE_GAIN * travelled
                    scaleX = grow
                    scaleY = grow
                    alpha = (1f - EXIT_FADE_RATE * travelled).coerceIn(0f, 1f)
                    rotationX = 12f * travelled
                }
            }
            .clip(RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp))
            .background(ContinueColors.SurfaceRaised)
            .border(
                width = 1.dp,
                color = ContinueColors.HairlineLight,
                shape = RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Same fallback as GameCard: the initial is painted underneath, so a card whose art
        // hasn't arrived still reads as a game rather than as an empty slab.
        Text(
            text = entry.name.take(1).uppercase(),
            style = ContinueTextStyles.displayXl,
            color = ContinueColors.TextTertiary,
        )
        if (entry.coverUrl != null) {
            AsyncImage(
                // GRID (528x704), not HERO, on purpose. The front card here is around 600px wide
                // on a 1080p phone, so HERO would buy roughly nothing — IgdbImage's own
                // measurements note IGDB upscales past the source, which for a typical cover is
                // only 600x800. Sharing the token with the grid means one cache entry per game,
                // so switching views is instant and promoting a card to the front never
                // re-fetches and flashes.
                model = IgdbImage.at(entry.coverUrl, IgdbImage.GRID),
                contentDescription = entry.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/**
 * The front card's identity, below the pile. Deliberately *not* printed on the cards: six
 * stacked title strips is six competing labels, and a game case doesn't caption itself.
 *
 * Dims through the hand-off so the caption is never seen attached to the wrong cover.
 */
@Composable
private fun StackCaption(
    entry: PileEntryWithGame,
    index: Int,
    total: Int,
    positionProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val meta = listOfNotNull(entry.ownedPlatform, entry.playtimeLabel).joinToString(" · ")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ContinueSpacing.XL.dp)
            .graphicsLayer { alpha = (1f - 2f * abs(index - positionProvider())).coerceIn(0f, 1f) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${index + 1} / $total",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextTertiary,
        )
        Text(
            text = entry.name,
            style = ContinueTextStyles.titleL,
            color = ContinueColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta.isNotEmpty()) {
            Text(
                text = meta.uppercase(),
                style = ContinueTextStyles.label,
                color = ContinueColors.TextSecondary,
            )
        }
    }
}
