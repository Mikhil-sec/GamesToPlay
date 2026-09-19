package com.mikhilnaika.continueapp.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikhilnaika.continueapp.core.audio.LocalArcadeAudio
import com.mikhilnaika.continueapp.core.audio.MusicCue
import com.mikhilnaika.continueapp.core.audio.MusicTrack
import com.mikhilnaika.continueapp.core.audio.Sfx
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.ui.ArcadeButton
import kotlinx.coroutines.launch
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles

/**
 * Under 40 seconds end to end — docs/02-PRODUCT-SPEC.md §8.
 *
 * [onFinished] receives whether the user asked to go straight to search, so "SEARCH FOR GAMES"
 * actually lands on DISCOVER instead of dumping them on an empty PILE to find it themselves.
 */
@Composable
fun OnboardingScreen(
    onFinished: (startAtDiscover: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Attract mode: the title theme runs under all three steps and fades out as the pile opens.
    MusicCue(MusicTrack.TITLE)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContinueSpacing.XL.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state.step) {
            OnboardingStep.COLD_OPEN -> ColdOpenStep(onContinue = viewModel::onColdOpenContinue)
            OnboardingStep.PILE_SIZE -> PileSizeStep(onAnswer = viewModel::onPileSizeAnswered)
            OnboardingStep.SEED -> SeedStep(
                onSearch = { viewModel.completeOnboarding { onFinished(true) } },
                onSkip = { viewModel.completeOnboarding { onFinished(false) } },
            )
        }
    }
}

/**
 * The cold open — docs/02-PRODUCT-SPEC.md §8: "the CRT powers on, CONTINUE? glows, coin-slot
 * sound". The first thing anyone sees, so it's the one screen that gets a real entrance.
 *
 * A CRT doesn't fade in: the beam draws a bright horizontal line, which then opens vertically
 * into the picture. That is two scale animations, ~0.5s together. Tapping anywhere finishes it
 * at once — the working agreement is that no animation ever makes a fast user wait.
 */
@Composable
private fun ColdOpenStep(onContinue: () -> Unit) {
    val audio = LocalArcadeAudio.current
    val scaleX = remember { Animatable(0.02f) }
    val scaleY = remember { Animatable(0.006f) }
    val glare = remember { Animatable(1f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        audio.play(Sfx.CRT_ON)
        scaleX.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        launch { glare.animateTo(0f, tween(700)) }
        scaleY.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
    }

    // The marquee breathes and INSERT COIN blinks, the two things an attract screen always did.
    val infinite = rememberInfiniteTransition(label = "attract")
    val glow by infinite.animateFloat(
        initialValue = 8f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "marqueeGlow",
    )
    val blink by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
        label = "insertCoinBlink",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures {
                    scope.launch { scaleX.snapTo(1f); scaleY.snapTo(1f); glare.snapTo(0f) }
                }
            }
            .graphicsLayer {
                this.scaleX = scaleX.value
                this.scaleY = scaleY.value
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CONTINUE?",
                style = ContinueTextStyles.displayXl.copy(
                    shadow = Shadow(color = ContinueColors.AccentCoin, blurRadius = glow),
                ),
                color = ContinueColors.AccentCoin,
                textAlign = TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
            Text(
                text = "The games you started deserve an ending.",
                style = ContinueTextStyles.body,
                color = ContinueColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XXL.dp))
            ArcadeButton(
                text = "INSERT COIN",
                onClick = {
                    audio.play(Sfx.COIN)
                    onContinue()
                },
                modifier = Modifier.fillMaxWidth().alpha(blink),
            )
        }
        // The beam's glare: the picture starts white-hot and cools to its real colours.
        if (glare.value > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(ContinueColors.TextPrimary.copy(alpha = glare.value * 0.85f)),
            )
        }
    }
}

@Composable
private fun PileSizeStep(onAnswer: (PileSizeAnswer) -> Unit) {
    Text(
        text = "HOW BIG IS YOUR PILE?",
        style = ContinueTextStyles.titleL,
        color = ContinueColors.TextPrimary,
        textAlign = TextAlign.Center,
    )
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.MD.dp))
    // The share target is how this app is *meant* to be used, and onboarding used to end
    // without ever mentioning it — leaving the headline feature discoverable only by accident.
    Text(
        text = "Already the best way: when a game shows up in a TikTok or a YouTube video, " +
            "share it and pick CONTINUE?. It'll work out which game it is.",
        style = ContinueTextStyles.body,
        color = ContinueColors.TextSecondary,
        textAlign = TextAlign.Center,
    )
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.LG.dp))
    Column(verticalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { onAnswer(PileSizeAnswer.UNDER_20) }, modifier = Modifier.fillMaxWidth()) {
            Text("UNDER 20")
        }
        Button(onClick = { onAnswer(PileSizeAnswer.TWENTY_TO_100) }, modifier = Modifier.fillMaxWidth()) {
            Text("20–100")
        }
        OutlinedButton(onClick = { onAnswer(PileSizeAnswer.DONT_ASK) }, modifier = Modifier.fillMaxWidth()) {
            Text("DON'T ASK")
        }
    }
}

@Composable
private fun SeedStep(onSearch: () -> Unit, onSkip: () -> Unit) {
    Text(
        text = "SEED YOUR PILE",
        style = ContinueTextStyles.titleL,
        color = ContinueColors.TextPrimary,
        textAlign = TextAlign.Center,
    )
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
    Column(verticalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) { Text("SEARCH FOR GAMES") }
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("SKIP") }
    }
}
