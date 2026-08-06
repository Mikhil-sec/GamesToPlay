package com.mikhilnaika.continueapp.feature.pile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.ui.EmptyState
import com.mikhilnaika.continueapp.core.ui.GameCard

private val PILE_TABS = listOf(
    PileState.BACKLOG to "THE PILE",
    PileState.PLAYING to "NOW PLAYING",
    PileState.COMPLETED to "CLEARED",
    PileState.DROPPED to "RETIRED",
    PileState.WISHLIST to "WANTED",
)

@Composable
fun PileScreen(
    modifier: Modifier = Modifier,
    viewModel: PileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        PileTabs(selected = state.selectedState, onSelect = viewModel::selectTab)

        TimeBudgetBar(
            totalHours = state.totalHours,
            totalGames = state.totalGames,
            finishCopy = state.timeBudgetFinishCopy,
            modifier = Modifier.padding(horizontal = ContinueSpacing.LG.dp, vertical = ContinueSpacing.SM.dp),
        )

        SortFilterRow(
            sort = state.sort,
            onSortSelected = viewModel::setSort,
            modifier = Modifier.padding(horizontal = ContinueSpacing.LG.dp),
        )

        if (state.entries.isEmpty() && !state.isLoading) {
            EmptyState(
                headline = when (state.selectedState) {
                    PileState.BACKLOG -> "INSERT GAME TO BEGIN"
                    PileState.PLAYING -> "NOTHING IN THE CABINET YET"
                    PileState.COMPLETED -> "NOTHING CLEARED YET"
                    PileState.DROPPED -> "NOTHING RETIRED"
                    PileState.WISHLIST -> "NOTHING WANTED YET"
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(ContinueSpacing.LG.dp),
                horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
                verticalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
            ) {
                items(state.entries, key = { it.entryId }) { entry ->
                    PileGameCard(entry = entry, onClick = { viewModel.moveToPlaying(entry.entryId) })
                }
            }
        }
    }

    state.swapPrompt?.let { prompt ->
        SwapDialog(
            prompt = prompt,
            onSwap = viewModel::confirmSwap,
            onDismiss = viewModel::dismissSwapPrompt,
        )
    }
}

@Composable
private fun PileTabs(selected: PileState, onSelect: (PileState) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = ContinueSpacing.LG.dp, vertical = ContinueSpacing.SM.dp),
        horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
    ) {
        PILE_TABS.forEach { (pileState, label) ->
            FilterChip(
                selected = selected == pileState,
                onClick = { onSelect(pileState) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun SortFilterRow(sort: PileSort, onSortSelected: (PileSort) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(bottom = ContinueSpacing.SM.dp),
        horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
    ) {
        FilterChip(
            selected = sort == PileSort.LENGTH_SHORT_FIRST,
            onClick = { onSortSelected(PileSort.LENGTH_SHORT_FIRST) },
            label = { Text("SHORTEST FIRST") },
        )
        FilterChip(
            selected = sort == PileSort.DATE_ADDED,
            onClick = { onSortSelected(PileSort.DATE_ADDED) },
            label = { Text("RECENT") },
        )
        FilterChip(
            selected = sort == PileSort.TITLE,
            onClick = { onSortSelected(PileSort.TITLE) },
            label = { Text("A-Z") },
        )
    }
}

@Composable
private fun PileGameCard(entry: PileEntryWithGame, onClick: () -> Unit) {
    val hours = entry.playtimeHoursNormally
    GameCard(
        title = entry.name,
        coverUrl = entry.coverUrl,
        subtitle = if (hours != null) "$hours HRS" else null,
        onClick = onClick,
    )
}

@Composable
private fun SwapDialog(prompt: SwapPrompt, onSwap: (Long) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("YOUR CABINET ONLY FITS 3") },
        text = {
            Column {
                Text("What are you swapping out?")
                prompt.currentlyPlaying.forEach { playing ->
                    TextButton(onClick = { onSwap(playing.entryId) }) {
                        Text("Swap out ${playing.name}")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
        containerColor = ContinueColors.SurfaceRaised,
        textContentColor = ContinueColors.TextSecondary,
        titleContentColor = ContinueColors.TextPrimary,
    )
}
