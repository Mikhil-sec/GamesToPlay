package com.mikhilnaika.continueapp.feature.rank

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.data.RankBucket
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.ArcadeButton

/** docs/02-PRODUCT-SPEC.md §5 "RANK — pairwise ranking". */
@Composable
fun RankScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RankViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ContinueColors.SurfaceVoid)
            .padding(ContinueSpacing.LG.dp),
    ) {
        when (state.phase) {
            RankPhase.LOADING -> Unit
            RankPhase.BUCKET_SELECT -> BucketSelect(gameName = state.gameName, onSelect = viewModel::selectBucket)
            RankPhase.COMPARING -> Comparing(state = state, onChoose = viewModel::chooseWinner)
            RankPhase.VERDICT -> VerdictStep(
                state = state,
                onVerdictChanged = viewModel::setVerdictText,
                onWouldReplayChanged = viewModel::setWouldReplay,
                onSave = viewModel::save,
            )
            RankPhase.DONE -> DoneStep(state = state, onFinished = onFinished)
        }
    }
}

@Composable
private fun BucketSelect(gameName: String, onSelect: (RankBucket) -> Unit) {
    Text(text = "HOW WAS $gameName?", style = ContinueTextStyles.displayL, color = ContinueColors.TextPrimary)
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
    val buckets = listOf(
        RankBucket.LOVED to "LOVED IT",
        RankBucket.LIKED to "LIKED IT",
        RankBucket.FINE to "IT WAS FINE",
        RankBucket.NAH to "NAH",
    )
    LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp), verticalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
        items(buckets) { (bucket, label) ->
            Box(
                modifier = Modifier
                    .aspectRatio(1.4f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ContinueColors.SurfaceRaised)
                    .clickable { onSelect(bucket) },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = label, style = ContinueTextStyles.titleL, color = ContinueColors.TextPrimary, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Comparing(state: RankUiState, onChoose: (Boolean) -> Unit) {
    Text(
        text = "WHICH DID YOU ENJOY MORE?",
        style = ContinueTextStyles.titleL,
        color = ContinueColors.TextPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.MD.dp),
    ) {
        ComparisonCard(name = state.gameName, coverUrl = state.coverUrl, onClick = { onChoose(true) }, modifier = Modifier.weight(1f))
        ComparisonCard(
            name = state.currentOpponent?.name ?: "",
            coverUrl = state.currentOpponent?.coverUrl,
            onClick = { onChoose(false) },
            modifier = Modifier.weight(1f),
        )
    }
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.LG.dp))
    Text(
        text = "COMPARISON ${state.comparisonsUsed + 1} OF $MAX_COMPARISONS",
        style = ContinueTextStyles.label,
        color = ContinueColors.TextTertiary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ComparisonCard(name: String, coverUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ContinueColors.SurfaceCabinet)
            .clickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).background(ContinueColors.SurfaceRaised)) {
            if (coverUrl != null) {
                AsyncImage(model = coverUrl, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Text(
            text = name,
            style = ContinueTextStyles.titleM,
            color = ContinueColors.TextPrimary,
            maxLines = 2,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
private fun VerdictStep(
    state: RankUiState,
    onVerdictChanged: (String) -> Unit,
    onWouldReplayChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    Text(text = "ONE-LINE VERDICT", style = ContinueTextStyles.titleL, color = ContinueColors.TextPrimary)
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))
    OutlinedTextField(
        value = state.verdictText,
        onValueChange = onVerdictChanged,
        placeholder = { Text("Your hot take, in 140 characters") },
        modifier = Modifier.fillMaxWidth(),
    )
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.MD.dp))
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = state.wouldReplay, onCheckedChange = onWouldReplayChanged)
        Text(text = "WOULD REPLAY", style = ContinueTextStyles.label, color = ContinueColors.TextSecondary)
    }
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
    ArcadeButton(text = "SAVE", onClick = onSave)
}

@Composable
private fun DoneStep(state: RankUiState, onFinished: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "YOUR #${state.finalPosition} OF ALL TIME",
                style = ContinueTextStyles.displayL,
                color = ContinueColors.AccentCoin,
                textAlign = TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.LG.dp))
            ArcadeButton(text = "DONE", onClick = onFinished)
        }
    }
}
