package com.mikhilnaika.continueapp.feature.pile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.mikhilnaika.continueapp.core.data.UserPreferencesRepository
import com.mikhilnaika.continueapp.core.data.dao.PileEntryWithGame
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.share.ShareLinks
import com.mikhilnaika.continueapp.core.ui.EmptyState
import com.mikhilnaika.continueapp.core.ui.GameCard
import com.mikhilnaika.continueapp.core.ui.GameDatesDialog
import com.mikhilnaika.continueapp.core.ui.IgdbAttribution
import com.mikhilnaika.continueapp.core.ui.LocalHaptics
import com.mikhilnaika.continueapp.core.util.CalendarDates
import com.mikhilnaika.continueapp.core.util.GameFacet
import com.mikhilnaika.continueapp.core.util.playtimeLabel
import com.mikhilnaika.continueapp.feature.stacks.StacksViewModel
import kotlin.math.roundToInt

private val PILE_TABS = listOf(
    PileState.BACKLOG to "THE PILE",
    PileState.PLAYING to "NOW PLAYING",
    PileState.COMPLETED to "CLEARED",
    PileState.DROPPED to "RETIRED",
    PileState.WISHLIST to "WANTED",
)

/** Enough room for the empty-state art to breathe without stealing the whole viewport. */
private val EMPTY_STATE_HEIGHT = 240.dp

@Composable
fun PileScreen(
    modifier: Modifier = Modifier,
    onGameCompleted: (Long) -> Unit = {},
    onOpenStacks: () -> Unit = {},
    onOpenShare: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    viewModel: PileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val stacksViewModel: StacksViewModel = hiltViewModel()
    val stacksState by stacksViewModel.state.collectAsState()
    var actionMenuEntry by remember { mutableStateOf<PileEntryWithGame?>(null) }
    var removeConfirmEntry by remember { mutableStateOf<PileEntryWithGame?>(null) }
    var datesEntry by remember { mutableStateOf<PileEntryWithGame?>(null) }
    var editingHoursPerWeek by remember { mutableStateOf(false) }

    var controlsExpanded by rememberSaveable { mutableStateOf(false) }

    val haptics = LocalHaptics.current

    val emptyHeadline = when (state.selectedState) {
        PileState.BACKLOG -> "INSERT GAME TO BEGIN"
        PileState.PLAYING -> "NOTHING IN THE CABINET YET"
        PileState.COMPLETED -> "NOTHING CLEARED YET"
        PileState.DROPPED -> "NOTHING RETIRED"
        PileState.WISHLIST -> "NOTHING WANTED YET"
    }
    // The empty pile is the app's best teaching moment and it was spending it on a slogan.
    // Nothing anywhere in the app mentioned the share target — the feature the whole product is
    // built around — so a user who never thought to look in the OS share sheet simply never
    // found it. See docs/10-BUILD-STATUS.md 2026-08-19.
    val emptySupporting = when (state.selectedState) {
        PileState.BACKLOG ->
            "Saw a game in a video? Hit share in TikTok, YouTube or Reddit and pick CONTINUE? — " +
                "it reads the title and adds it here. Or search for one in DISCOVER."
        PileState.WISHLIST -> "Games you don't own yet. Share or search one in to start the list."
        else -> null
    }
    val isEmpty = state.entries.isEmpty() && !state.isLoading

    // Tabs, time budget, controls and filters are *items in the list*, not a fixed header above
    // it. As a fixed header they were unscrollable dead weight: expanding the filters on a phone
    // left the games squeezed into whatever was left (sometimes nothing), and even on a tablet
    // you could only ever scroll the covers, never the chrome. Now the whole screen scrolls as
    // one surface and the filters can always be scrolled past.
    val header: @Composable () -> Unit = {
        PileHeader(
            state = state,
            controlsExpanded = controlsExpanded,
            onToggleControls = { controlsExpanded = !controlsExpanded },
            onSelectTab = viewModel::selectTab,
            onSort = viewModel::setSort,
            onTogglePlatform = viewModel::togglePlatformFilter,
            onToggleFacet = viewModel::toggleFacetFilter,
            onLengthBucket = viewModel::setLengthBucketFilter,
            onClearFilters = viewModel::clearFilters,
            onSetViewMode = viewModel::setViewMode,
            onOpenStacks = onOpenStacks,
            onOpenShare = onOpenShare,
            onOpenStats = onOpenStats,
            onEditHoursPerWeek = { editingHoursPerWeek = true },
        )
    }

    when (state.viewMode) {
        // STACK owns a vertical drag gesture of its own, so it cannot live inside a scrolling
        // parent the way GRID and LIST do — the two would fight for every drag. Its header is a
        // fixed block instead, and the stack takes whatever height is left and sizes itself to
        // it, so expanding the filters shrinks the cards rather than pushing them off-screen.
        PileViewMode.STACK -> Column(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = ContinueSpacing.LG.dp)) { header() }
            if (isEmpty) {
                EmptyState(
                    headline = emptyHeadline,
                    supporting = emptySupporting,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            } else {
                PileStackView(
                    entries = state.entries,
                    onSelect = { actionMenuEntry = it },
                    // Any of these makes it a different pile, and leaving the stack parked on
                    // card 40 of a list that just became three cards long is disorienting.
                    resetKey = listOf(
                        state.selectedState,
                        state.sort,
                        state.platformFilters,
                        state.facetFilters,
                        state.lengthBucketFilter,
                    ),
                    haptics = haptics,
                    showHint = state.showStackHint,
                    onHintDismissed = viewModel::markStackHintSeen,
                    modifier = Modifier
                        .weight(1f)
                        // Same 24dp DRAW-button overhang the lazy lists pad for below.
                        .padding(bottom = ContinueSpacing.XXL.dp),
                )
            }
        }

        PileViewMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = ContinueSpacing.LG.dp,
                    end = ContinueSpacing.LG.dp,
                    top = ContinueSpacing.SM.dp,
                    // The raised DRAW button overhangs the nav bar by 24dp, so the last row needs
                    // clearance or it sits underneath it.
                    bottom = ContinueSpacing.XXL.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
                verticalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "header") { header() }
                if (isEmpty) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                        EmptyState(
                            headline = emptyHeadline,
                            supporting = emptySupporting,
                            modifier = Modifier.fillMaxWidth().height(EMPTY_STATE_HEIGHT),
                        )
                    }
                } else {
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
                    item(span = { GridItemSpan(maxLineSpan) }, key = "igdb") { IgdbAttribution() }
                }
            }
        }

        PileViewMode.LIST -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = ContinueSpacing.LG.dp,
                    end = ContinueSpacing.LG.dp,
                    top = ContinueSpacing.SM.dp,
                    bottom = ContinueSpacing.XXL.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(ContinueSpacing.XS.dp),
            ) {
                item(key = "header") { header() }
                if (isEmpty) {
                    item(key = "empty") {
                        EmptyState(
                            headline = emptyHeadline,
                            supporting = emptySupporting,
                            modifier = Modifier.fillMaxWidth().height(EMPTY_STATE_HEIGHT),
                        )
                    }
                } else {
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
                    item(key = "igdb") { IgdbAttribution() }
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
            onEditDates = {
                actionMenuEntry = null
                datesEntry = entry
            },
            onShare = { campaign ->
                actionMenuEntry = null
                viewModel.shareGame(entry.gameId, entry.name, campaign)
            },
            onRemove = {
                actionMenuEntry = null
                removeConfirmEntry = entry
            },
        )
    }

    datesEntry?.let { entry ->
        GameDatesDialog(
            gameName = entry.name,
            initialStartedAt = entry.startedAt,
            initialFinishedAt = entry.finishedAt,
            title = "EDIT DATES",
            supporting = if (entry.state != PileState.COMPLETED) {
                "Setting a cleared date files this game under CLEARED."
            } else {
                null
            },
            onDismiss = { datesEntry = null },
            onSave = { startedAt, finishedAt ->
                datesEntry = null
                viewModel.setDates(entry.entryId, startedAt, finishedAt)
            },
        )
    }

    if (editingHoursPerWeek) {
        HoursPerWeekDialog(
            totalHours = state.totalHours,
            hoursPerWeek = state.hoursPerWeek,
            onDismiss = { editingHoursPerWeek = false },
            onSave = { hours ->
                editingHoursPerWeek = false
                viewModel.setHoursPerWeek(hours)
            },
        )
    }

    removeConfirmEntry?.let { entry ->
        RemoveConfirmDialog(
            entry = entry,
            onDismiss = { removeConfirmEntry = null },
            onConfirm = {
                removeConfirmEntry = null
                viewModel.removeFromPile(entry.entryId)
            },
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
    onEditDates: () -> Unit,
    onShare: (ShareLinks.Campaign) -> Unit,
    onRemove: () -> Unit,
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

                // The friend loop's outbound half. Two verbs rather than one "share": the
                // difference between handing someone a game and betting they won't finish it
                // is the entire social dynamic this app is about, and it costs one extra row.
                Text(
                    text = "SEND TO A FRIEND",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextTertiary,
                    modifier = Modifier.padding(top = ContinueSpacing.SM.dp),
                )
                TextButton(onClick = { onShare(ShareLinks.Campaign.PICK) }, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RECOMMEND IT",
                            style = ContinueTextStyles.body,
                            color = ContinueColors.AccentNeon,
                        )
                        Text(
                            text = "They tap the link, it lands on their pile",
                            style = ContinueTextStyles.label,
                            color = ContinueColors.TextTertiary,
                        )
                    }
                }
                TextButton(onClick = { onShare(ShareLinks.Campaign.DARE) }, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DARE THEM TO FINISH IT",
                            style = ContinueTextStyles.body,
                            color = ContinueColors.AccentNeon,
                        )
                        Text(
                            text = "Same link, worse manners",
                            style = ContinueTextStyles.label,
                            color = ContinueColors.TextTertiary,
                        )
                    }
                }

                // Not a destination either, but not destructive — it gets its own line above
                // the OR, with whatever dates the game already carries spelled out so the user
                // can see there's something here worth editing.
                Text(
                    text = "DATES",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextTertiary,
                    modifier = Modifier.padding(top = ContinueSpacing.SM.dp),
                )
                TextButton(onClick = onEditDates, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EDIT DATES",
                            style = ContinueTextStyles.body,
                            color = ContinueColors.AccentCoin,
                        )
                        Text(
                            text = datesSummary(entry),
                            style = ContinueTextStyles.label,
                            color = ContinueColors.TextTertiary,
                        )
                    }
                }

                // Last, under its own heading, in the one hot colour the app reserves for
                // destructive things — it is *not* a sixth destination, and putting it in the
                // MOVE TO list would invite exactly the mis-tap it exists to undo.
                Text(
                    text = "OR",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextTertiary,
                    modifier = Modifier.padding(top = ContinueSpacing.SM.dp),
                )
                TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "REMOVE FROM PILE",
                            style = ContinueTextStyles.body,
                            color = ContinueColors.AccentHot,
                        )
                        Text(
                            text = "Added by mistake — erase it completely",
                            style = ContinueTextStyles.label,
                            color = ContinueColors.TextTertiary,
                        )
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

/**
 * How many hours a week you actually play.
 *
 * "FINISHED BY 2029" is the line the whole PILE screen is built around, and its only input was a
 * hardcoded 6 that no UI could reach — so the app told a student on holiday and someone with a
 * newborn the same thing. `PileViewModel.setHoursPerWeek` existed and had no callers.
 *
 * The projection updates **as the slider moves**, through the same [finishByCopy] the bar itself
 * uses rather than a second copy of the arithmetic: the point of the control is the sentence, not
 * the number, and watching a decade collapse into a year as you drag is the honest way to show
 * what the setting means.
 */
@Composable
private fun HoursPerWeekDialog(
    totalHours: Int,
    hoursPerWeek: Float,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit,
) {
    var hours by remember { mutableFloatStateOf(hoursPerWeek) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("HOW MUCH DO YOU PLAY?") },
        text = {
            Column {
                Text(
                    text = "${hours.roundToInt()} HOURS A WEEK",
                    style = ContinueTextStyles.monoL,
                    color = ContinueColors.AccentCoin,
                )
                Slider(
                    value = hours,
                    onValueChange = { hours = it },
                    valueRange = UserPreferencesRepository.MIN_HOURS_PER_WEEK..
                        UserPreferencesRepository.MAX_HOURS_PER_WEEK,
                    // One stop per hour: the projection is a rough one either way, and a
                    // continuous slider that lands on 6.37 h/week implies a precision this
                    // estimate does not have.
                    steps = (UserPreferencesRepository.MAX_HOURS_PER_WEEK -
                        UserPreferencesRepository.MIN_HOURS_PER_WEEK).toInt() - 1,
                )
                Text(
                    text = finishByCopy(totalHours, hours.roundToInt().toFloat()),
                    style = ContinueTextStyles.body,
                    color = ContinueColors.TextSecondary,
                )
                Text(
                    text = "Only affects the estimate on this bar. Nothing else changes.",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextTertiary,
                    modifier = Modifier.padding(top = ContinueSpacing.SM.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(hours.roundToInt().toFloat()) }) {
                Text("SAVE", color = ContinueColors.AccentCoin)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
        containerColor = ContinueColors.SurfaceRaised,
        textContentColor = ContinueColors.TextSecondary,
        titleContentColor = ContinueColors.TextPrimary,
    )
}

/** "Started 3 Mar 2024 · cleared 18 Apr 2024", or the invitation to say so. */
private fun datesSummary(entry: PileEntryWithGame): String {
    val started = entry.startedAt?.let { "started ${CalendarDates.format(it)}" }
    val cleared = entry.finishedAt?.let { "cleared ${CalendarDates.format(it)}" }
    val known = listOfNotNull(started, cleared)
    return if (known.isEmpty()) "Played it years ago? Say when." else known.joinToString(" · ")
}

/**
 * The one destructive confirm in PILE.
 *
 * Removal is the only action here that can't be walked back from another tab — every "MOVE TO"
 * has an obvious inverse, this has none — so it gets a second tap and the game's name spelled
 * out, and the safe choice is the one sitting where a dialog's default goes.
 */
@Composable
private fun RemoveConfirmDialog(
    entry: PileEntryWithGame,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("REMOVE FROM PILE?") },
        text = {
            Text(
                text = "${entry.name} will be erased from your pile, its stacks and your " +
                    "rankings. This can't be undone — RETIRED is the one to use if you're just " +
                    "letting the game go.",
                style = ContinueTextStyles.body,
                color = ContinueColors.TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("REMOVE", color = ContinueColors.AccentHot)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("KEEP IT") } },
        containerColor = ContinueColors.SurfaceRaised,
        textContentColor = ContinueColors.TextSecondary,
        titleContentColor = ContinueColors.TextPrimary,
    )
}

/**
 * Everything above the games, as one scrollable block: state tabs, the time budget, the
 * action row, and the collapsible sort/filter chips.
 */
@Composable
private fun PileHeader(
    state: PileUiState,
    controlsExpanded: Boolean,
    onToggleControls: () -> Unit,
    onSelectTab: (PileState) -> Unit,
    onSort: (PileSort) -> Unit,
    onTogglePlatform: (String) -> Unit,
    onToggleFacet: (GameFacet) -> Unit,
    onLengthBucket: (LengthBucket) -> Unit,
    onClearFilters: () -> Unit,
    onSetViewMode: (PileViewMode) -> Unit,
    onOpenStacks: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenStats: () -> Unit,
    onEditHoursPerWeek: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // The five state tabs get a full-width scrolling strip of their own. They used to share
        // one un-scrollable Row with three icon buttons, which fits a tablet and badly breaks a
        // phone: Row hands the leftovers zero width, so "NOW PLAYING" rendered as a squeezed
        // vertical sliver and the last three tabs were unreachable entirely.
        PileTabs(selected = state.selectedState, onSelect = onSelectTab)

        // The bar has carried an `onExpand` hook since it was written and nothing had ever
        // passed one, so the chevron never rendered and the projection was un-editable. It now
        // opens the only input that projection has.
        TimeBudgetBar(
            totalHours = state.totalHours,
            totalGames = state.totalGames,
            finishCopy = state.timeBudgetFinishCopy,
            modifier = Modifier.padding(vertical = ContinueSpacing.SM.dp),
            onExpand = onEditHoursPerWeek,
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SortFilterToggle(
                expanded = controlsExpanded,
                activeFilterCount = state.activeFilterCount,
                onToggle = onToggleControls,
            )
            Box(modifier = Modifier.weight(1f))
            // Deliberately **not** a fourth icon button. `Row` measures its non-weighted
            // children first and hands the leftovers whatever is left — which on a 360dp phone
            // is nothing, because a "SORT & FILTER" chip plus four 48dp buttons is about 350dp
            // inside 328dp of usable width. That is the same proportion error that made three of
            // PILE's tabs unreachable on 2026-08-14 and put the DRAW button on top of the
            // DISCOVER label on 2026-08-15, and both times a tablet absorbed it. STATS is
            // reached from the filter panel below and from YOU instead.
            IconButton(onClick = onOpenStacks) {
                Icon(Icons.Filled.Layers, contentDescription = "Stacks", tint = ContinueColors.TextSecondary)
            }
            IconButton(onClick = onOpenShare) {
                Icon(Icons.Filled.Share, contentDescription = "Share the pile", tint = ContinueColors.TextSecondary)
            }
            ViewModeToggle(mode = state.viewMode, onSelect = onSetViewMode)
        }

        // Sort and filters cost several rows of chips on a phone — more vertical space than the
        // games themselves. Collapsed by default; the toggle carries the active-filter count so
        // hiding them never hides *that they're on*.
        AnimatedVisibility(visible = controlsExpanded) {
            Column {
                SortFilterRow(
                    sort = state.sort,
                    onSortSelected = onSort,
                    modifier = Modifier.padding(vertical = ContinueSpacing.XS.dp),
                )

                FiltersSection(
                    state = state,
                    onTogglePlatform = onTogglePlatform,
                    onToggleFacet = onToggleFacet,
                    onLengthBucket = onLengthBucket,
                    onClearFilters = onClearFilters,
                    onOpenStats = onOpenStats,
                )
            }
        }
    }
}

@Composable
private fun PileTabs(selected: PileState, onSelect: (PileState) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = ContinueSpacing.SM.dp),
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

/** The disclosure for SORT & FILTER. Carries the active-filter count when collapsed. */
@Composable
private fun SortFilterToggle(expanded: Boolean, activeFilterCount: Int, onToggle: () -> Unit) {
    FilterChip(
        selected = expanded || activeFilterCount > 0,
        onClick = onToggle,
        label = {
            Text(if (activeFilterCount > 0) "SORT & FILTER - $activeFilterCount" else "SORT & FILTER")
        },
        trailingIcon = {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Hide sort and filters" else "Show sort and filters",
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

/**
 * Every sort the enum declares, in its own order.
 *
 * This row used to hardcode three of the six, and two of the three it left out did nothing when
 * reached by any other route — TOP RATED returned the list untouched and NEWEST sorted by date
 * added. Generating the row from `PileSort` means an option can't exist without being offered,
 * and `PileFiltering.sort` is exhaustive over the same enum, so it can't be offered without
 * being implemented.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SortFilterRow(sort: PileSort, onSortSelected: (PileSort) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(bottom = ContinueSpacing.XS.dp)) {
        Text(text = "SORT", style = ContinueTextStyles.label, color = ContinueColors.TextTertiary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
            PileSort.entries.forEach { option ->
                FilterChip(
                    selected = sort == option,
                    onClick = { onSortSelected(option) },
                    label = { Text(option.label) },
                )
            }
        }
    }
}

@Composable
private fun PileGameCard(entry: PileEntryWithGame, onClick: () -> Unit, onLongClick: () -> Unit) {
    GameCard(
        title = entry.name,
        coverUrl = entry.coverUrl,
        subtitle = entry.playtimeLabel,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

/**
 * One button, not three: it cycles, and shows the mode you'd switch *to*. Three side-by-side
 * icon buttons cost 144dp of a phone's header for a choice made once, and the un-picked ones
 * always read as disabled. Cycle order is the enum's own order, so the two can't drift.
 */
@Composable
private fun ViewModeToggle(mode: PileViewMode, onSelect: (PileViewMode) -> Unit, modifier: Modifier = Modifier) {
    val modes = PileViewMode.entries
    val next = modes[(mode.ordinal + 1) % modes.size]
    IconButton(onClick = { onSelect(next) }, modifier = modifier) {
        Icon(
            imageVector = when (next) {
                PileViewMode.STACK -> Icons.Filled.ViewCarousel
                PileViewMode.GRID -> Icons.Filled.GridView
                PileViewMode.LIST -> Icons.AutoMirrored.Filled.ViewList
            },
            contentDescription = when (next) {
                PileViewMode.STACK -> "Switch to stack view"
                PileViewMode.GRID -> "Switch to grid view"
                PileViewMode.LIST -> "Switch to list view"
            },
            tint = ContinueColors.TextSecondary,
        )
    }
}

/**
 * The filters, in three labelled groups — docs/02-PRODUCT-SPEC.md §1 "Sort & filter".
 *
 * Rebuilt after a closed tester reported the filters as inconsistent between categories and
 * with DRAW. Three things changed, and each was its own bug:
 *
 * 1. **The vocabulary.** Chips were IGDB's raw genre strings, which have no "horror" (that's a
 *    theme), no "FPS" (that's "Shooter"), and no "multiplayer" at all. They are now
 *    [GameFacet]s, matched across genres, themes and game modes — the same vocabulary DRAW's
 *    GENRE dial and the STATS screen use, so all three finally agree.
 * 2. **The chip set is stable.** It used to be built from the games in the current tab, so it
 *    changed every time you switched tab. It is now built from the whole pile.
 * 3. **Every chip carries its count in this tab**, and one that would match nothing says zero
 *    rather than silently vanishing — which is what made a leftover filter look like an empty
 *    pile.
 */
@Composable
private fun FiltersSection(
    state: PileUiState,
    onTogglePlatform: (String) -> Unit,
    onToggleFacet: (GameFacet) -> Unit,
    onLengthBucket: (LengthBucket) -> Unit,
    onClearFilters: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = ContinueSpacing.SM.dp)) {
        FilterGroup(
            title = "LENGTH",
            options = state.availableLengths,
            isSelected = { it == state.lengthBucketFilter },
            onToggle = onLengthBucket,
        )
        FilterGroup(
            title = "GENRE & MOOD",
            options = state.availableFacets,
            isSelected = { it in state.facetFilters },
            onToggle = onToggleFacet,
        )
        FilterGroup(
            title = "PLATFORM",
            options = state.availablePlatforms,
            isSelected = { it in state.platformFilters },
            onToggle = onTogglePlatform,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            // STATS is the filters drawn as a picture — same facets, same counts — so the panel
            // that sets them is where it belongs.
            TextButton(onClick = onOpenStats) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = ContinueColors.AccentCoin,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "  SEE THE STATS",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.AccentCoin,
                )
            }
            if (state.activeFilterCount > 0) {
                TextButton(onClick = onClearFilters) {
                    Text("CLEAR FILTERS", style = ContinueTextStyles.label, color = ContinueColors.AccentHot)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun <T> FilterGroup(
    title: String,
    options: List<FilterOption<T>>,
    isSelected: (T) -> Boolean,
    onToggle: (T) -> Unit,
) {
    if (options.isEmpty()) return
    Column(modifier = Modifier.padding(top = ContinueSpacing.SM.dp)) {
        Text(text = title, style = ContinueTextStyles.label, color = ContinueColors.TextTertiary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
            options.forEach { option ->
                val selected = isSelected(option.value)
                FilterChip(
                    selected = selected,
                    // A zero-count chip stays tappable when it is the one that is *on* — it has
                    // to be, or the only control that could undo it would disappear along with
                    // the games it is hiding.
                    enabled = !option.isEmptyHere || selected,
                    onClick = { onToggle(option.value) },
                    label = { Text(option.label + "  " + option.countInTab) },
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PileListRow(entry: PileEntryWithGame, onClick: () -> Unit, onLongClick: () -> Unit) {
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
                text = listOfNotNull(entry.ownedPlatform, entry.playtimeLabel).joinToString(" · "),
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
