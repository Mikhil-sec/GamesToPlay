package com.mikhilnaika.continueapp.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles

/** Under 40 seconds end to end — docs/02-PRODUCT-SPEC.md §8. */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

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
            OnboardingStep.SEED -> SeedStep(onDone = viewModel::onSeedStepDone)
            OnboardingStep.FIRST_DRAW -> FirstDrawStep(onDone = { viewModel.completeOnboarding(onFinished) })
        }
    }
}

@Composable
private fun ColdOpenStep(onContinue: () -> Unit) {
    Text(
        text = "CONTINUE?",
        style = ContinueTextStyles.displayXl,
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
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("INSERT COIN") }
}

@Composable
private fun PileSizeStep(onAnswer: (PileSizeAnswer) -> Unit) {
    Text(
        text = "HOW BIG IS YOUR PILE?",
        style = ContinueTextStyles.titleL,
        color = ContinueColors.TextPrimary,
        textAlign = TextAlign.Center,
    )
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
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
private fun SeedStep(onDone: () -> Unit) {
    Text(
        text = "SEED YOUR PILE",
        style = ContinueTextStyles.titleL,
        color = ContinueColors.TextPrimary,
        textAlign = TextAlign.Center,
    )
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
    Column(verticalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp), modifier = Modifier.fillMaxWidth()) {
        // Steam import's full review screen is week-4 roadmap scope (docs/06-BUILD-ROADMAP.md);
        // disabled here rather than half-built.
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) {
            Text("IMPORT FROM STEAM (COMING SOON)")
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("SEARCH FOR GAMES") }
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("SKIP") }
    }
}

@Composable
private fun FirstDrawStep(onDone: () -> Unit) {
    Text(
        text = "PULL THE LEVER",
        style = ContinueTextStyles.titleL,
        color = ContinueColors.TextPrimary,
        textAlign = TextAlign.Center,
    )
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))
    Text(
        text = "The full DRAW machine — dials, lever physics, the card deal — is a Week 3 build " +
            "(docs/06-BUILD-ROADMAP.md). This finishes onboarding and drops you into the app.",
        style = ContinueTextStyles.body,
        color = ContinueColors.TextSecondary,
        textAlign = TextAlign.Center,
    )
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("LET'S GO") }
}
