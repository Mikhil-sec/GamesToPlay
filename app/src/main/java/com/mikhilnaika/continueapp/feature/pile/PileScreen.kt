package com.mikhilnaika.continueapp.feature.pile

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.EmptyState
import com.mikhilnaika.continueapp.core.ui.GameCard
import com.mikhilnaika.continueapp.feature.stacks.StacksViewModel

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
    onGameCompleted: (Long) -> Unit = {},
    onOpenStacks: () -> Unit = {},
    onOpenShare: () -> Unit = {},
    viewModel: PileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val stacksViewModel: StacksViewModel = hiltViewModel()
    val stacksState by stacksViewModel.state.collectAsState()
    var actionMenuEntry by remember { mutableStateOf<PileEntryWithGame?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PileTabs(selected = state.selectedState, onSelect = viewModel::selectTab, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenStacks) {
                Icon(Icons.Filled.Layers, contentDescription = "Stacks", tint = ContinueColors.TextSecondary)
            }
            IconButton(onClick = onOpenShare) {
                Icon(Icons.Filled.Share, contentDescription = "Share the pile", tint = ContinueColors.TextSecondary)
            }
            ViewModeToggle(mode = state.viewMode, onSelect = viewModel::setViewMode)
        }

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

        FiltersRow(
            state = state,
            onPlatform = viewModel::setPlatformFilter,
            onGenre = viewModel::setGenreFilter,
            onLengthBucket = viewModel::setLengthBucketFilter,
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
        } else if (state.viewMode == PileViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(ContinueSpacing.LG.dp),
                horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
                verticalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
            ) {
                items(state.entries, key = { it.entryId }) { entry ->
                    PileGameCard(
                        entry = entry,
                        // Tapping used to fling a BACKLOG game straight into NOW PLAYING, which
                        // read as "the game vanished" — the card left the list with no
                        // confirmation and no way back. Both gestures now open the same
                        // chooser; moving a game is always a deliberate, labelled choice.
                        onClick = { actionMenuEntry = entry },
                        onLongClick = { actionMenuEntry = entry },
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = ContinueSpacing.LG.dp, vertical = ContinueSpacing.SM.dp),
                verticalArrangement = Arrangement.spacedBy(ContinueSpacing.XS.dp),
            ) {
                items(state.entries, key = { it.entryId }) { entry ->
                    PileListRow(
                        entry = entry,
                        // Tapping used to fling a BACKLOG game straight into NOW PLAYING, which
                        // read as "the game vanished" — the card left the list with no
                        // confirmation and no way back. Both gestures now open the same
                        // chooser; moving a game is always a deliberate, labelled choice.
                        onClick = { actionMenuEntry = entry },
                        onLongClick = { actionMenuEntry = entry },
                    )
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

    actionMenuEntry?.let { entry ->
        PileActionMenu(
            entry = entry,
            stacks = stacksState.stacks,
            onDismiss = { actionMenuEntry = null },
            onMove = { target ->
                actionMenuEntry = null
                // CLEARED is the one transition that isn't a quiet state change — it earns the
                // Credits Roll (docs/02-PRODUCT-SPEC.md §4), which grants coins and leads into
                // RANK. Routing it through the nav callback keeps that payoff intact.
                if (target == PileState.COMPLETED) onGameCompleted(entry.entryId)
                else viewModel.moveTo(entry.entryId, target)
            },
            onToggleStack = { stackId -> stacksViewModel.addGameToStack(stackId, entry.gameId) },
        )
    }
}

/** Label and one-line meaning for each destination, in the order they're offered. */
private val MOVE_TARGETS: List<Triple<PileState, String, String>> = listOf(
    Triple(PileState.PLAYING, "NOW PLAYING", "Start it — max 3 at once"),
    Triple(PileState.COMPLETED, "CLEARED", "Roll the credits"),
    Triple(PileState.BACKLOG, "THE PILE", "Owned, not started"),
    Triple(PileState.WISHLIST, "WANTED", "Don't own it yet"),
    Triple(PileState.DROPPED, "RETIRED", "Letting this one go"),
)

/**
 * The single place a game's state changes from PILE.
 *
 * Every destination is listed with what it means, and the game's current state is shown as a
 * disabled row rather than hidden — so the list doesn't reshuffle depending on where the game
 * already is, and "where is this game now?" is answerable without leaving the sheet.
 */
@Composable
private fun PileActionMenu(
    entry: PileEntryWithGame,
    stacks: List<com.mikhilnaika.continueapp.core.data.entity.StackEntity>,
    onDismiss: () -> Unit,
    onMove: (PileState) -> Unit,
    onToggleStack: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "MOVE TO",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextTertiary,
                )
                MOVE_TARGETS.forEach { (target, label, description) ->
                    val isCurrent = entry.state == target
                    TextButton(
                        onClick = { onMove(target) },
                        enabled = !isCurrent,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCurrent) "$label  ·  CURRENT" else label,
                                style = ContinueTextStyles.body,
                                color = if (isCurrent) ContinueColors.TextTertiary else ContinueColors.AccentCoin,
                            )
                            Text(
                                text = description,
                                style = ContinueTextStyles.label,
                                color = ContinueColors.TextTertiary,
                            )
                        }
                    }
                }
                if (stacks.isNotEmpty()) {
                    Text(
                        text = "ADD TO STACK",
                        style = ContinueTextStyles.label,
                        color = ContinueColors.TextTertiary,
                        modifier = Modifier.padding(top = ContinueSpacing.SM.dp),
                    )
                    stacks.forEach { stack ->
                        TextButton(onClick = { onToggleStack(stack.stackId) }) {
                            Text("${stack.emoji.orEmpty()} ${stack.name}".trim())
                        }
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
private fun PileGameCard(entry: PileEntryWithGame, onClick: () -> Unit, onLongClick: () -> Unit) {
    val hours = entry.playtimeHoursNormally
    GameCard(
        title = entry.name,
        coverUrl = entry.coverUrl,
        subtitle = if (hours != null) "$hours HRS" else null,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
private fun ViewModeToggle(mode: PileViewMode, onSelect: (PileViewMode) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(end = ContinueSpacing.LG.dp)) {
        IconButton(onClick = { onSelect(PileViewMode.GRID) }) {
            Icon(
                imageVector = Icons.Filled.GridView,
                contentDescription = "Grid view",
                tint = if (mode == PileViewMode.GRID) ContinueColors.AccentCoin else ContinueColors.TextTertiary,
            )
        }
        IconButton(onClick = { onSelect(PileViewMode.LIST) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ViewList,
                contentDescription = "List view",
                tint = if (mode == PileViewMode.LIST) ContinueColors.AccentCoin else ContinueColors.TextTertiary,
            )
        }
    }
}

/** docs/02-PRODUCT-SPEC.md §1 "Sort & filter" — platform · genre · length bucket. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FiltersRow(
    state: PileUiState,
    onPlatform: (String) -> Unit,
    onGenre: (String) -> Unit,
    onLengthBucket: (LengthBucket) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.availablePlatforms.isEmpty() && state.availableGenres.isEmpty()) return
    FlowRow(
        modifier = modifier.padding(bottom = ContinueSpacing.SM.dp),
        horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
    ) {
        LengthBucket.entries.forEach { bucket ->
            FilterChip(
                selected = state.lengthBucketFilter == bucket,
                onClick = { onLengthBucket(bucket) },
                label = { Text(bucket.label) },
            )
        }
        state.availablePlatforms.forEach { platform ->
            FilterChip(
                selected = state.platformFilter == platform,
                onClick = { onPlatform(platform) },
                label = { Text(platform.uppercase()) },
            )
        }
        state.availableGenres.forEach { genre ->
            FilterChip(
                selected = state.genreFilter == genre,
                onClick = { onGenre(genre) },
                label = { Text(genre.uppercase()) },
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PileListRow(entry: PileEntryWithGame, onClick: () -> Unit, onLongClick: () -> Unit) {
    val hours = entry.playtimeHoursNormally
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ContinueColors.SurfaceCabinet)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(ContinueSpacing.SM.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(6.dp))
                .background(ContinueColors.SurfaceRaised),
        ) {
            if (entry.coverUrl != null) {
                AsyncImage(
                    model = entry.coverUrl,
                    contentDescription = entry.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = ContinueSpacing.SM.dp)) {
            Text(
                text = entry.name,
                style = ContinueTextStyles.body,
                color = ContinueColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(entry.ownedPlatform, hours?.let { "$it HRS" }).joinToString(" · "),
                style = ContinueTextStyles.label,
                color = ContinueColors.TextSecondary,
            )
        }
    }
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
