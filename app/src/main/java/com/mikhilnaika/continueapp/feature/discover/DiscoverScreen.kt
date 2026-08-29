package com.mikhilnaika.continueapp.feature.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueShapes
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.network.dto.GameDto
import com.mikhilnaika.continueapp.core.ui.GameCard
import com.mikhilnaika.continueapp.core.ui.GameDatesDialog
import com.mikhilnaika.continueapp.core.util.IgdbImage
import com.mikhilnaika.continueapp.core.util.Playtime
import kotlinx.coroutines.delay

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Two step chooser for a long-pressed result: pick where it goes, then (for CLEARED) when.
    var addOptionsFor by remember { mutableStateOf<GameDto?>(null) }
    var backdateFor by remember { mutableStateOf<GameDto?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ContinueSpacing.LG.dp),
                placeholder = { Text("SEARCH FOR A GAME") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                // Clearing a search used to mean holding backspace across a whole game title,
                // and the field is the only way back to the rails — asked for directly in
                // closed-test feedback. Only present when there is something to clear, so it
                // never competes with the placeholder.
                trailingIcon = if (state.query.isNotEmpty()) {
                    {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = ContinueColors.TextSecondary,
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
            )

            if (state.query.isNotBlank()) {
                SearchResultsList(
                    results = state.searchResults,
                    isSearching = state.isSearching,
                    note = state.searchNote,
                    addedGameIds = state.addedGameIds,
                    onAdd = { viewModel.addToPile(it) },
                    onMore = { addOptionsFor = it },
                )
            } else {
                Rails(
                    rails = state.rails,
                    addedGameIds = state.addedGameIds,
                    onAdd = { viewModel.addToPile(it) },
                    onMore = { addOptionsFor = it },
                    onShowMore = viewModel::loadMoreRail,
                )
            }
        }

        AddedBanner(
            message = state.message,
            onDismiss = viewModel::consumeMessage,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    addOptionsFor?.let { game ->
        AddOptionsDialog(
            game = game,
            onDismiss = { addOptionsFor = null },
            onPick = { target ->
                addOptionsFor = null
                if (target == PileState.COMPLETED) backdateFor = game else viewModel.addToPile(game, target)
            },
        )
    }

    backdateFor?.let { game ->
        GameDatesDialog(
            gameName = game.name,
            initialStartedAt = null,
            initialFinishedAt = System.currentTimeMillis(),
            title = "LOG A GAME YOU'VE CLEARED",
            confirmLabel = "LOG IT",
            supporting = "Goes straight to CLEARED with these dates. Old clears don't pay coins.",
            onDismiss = { backdateFor = null },
            onSave = { startedAt, finishedAt ->
                backdateFor = null
                viewModel.addToPile(
                    game = game,
                    state = PileState.COMPLETED,
                    startedAt = startedAt,
                    // A clear with no date is not a clear — fall back to today rather than
                    // filing a game under CLEARED with nothing for STATS or THIS YEAR to count.
                    finishedAt = finishedAt ?: System.currentTimeMillis(),
                )
            },
        )
    }
}

/** How long a confirmation stays up. Long enough to read a game name, short enough that a
 * second add doesn't queue behind it. */
private const val BANNER_MS = 2200L

/**
 * The answer to "did that tap do anything?".
 *
 * Adding from DISCOVER was previously silent apart from a tick that vanished the moment you
 * left the screen, which is a large part of why a tester could add games, see nothing in PILE,
 * and have no way to tell which half of that was broken. Every tap now says either that the
 * game went in or that it was already there.
 */
@Composable
private fun AddedBanner(
    message: DiscoverMessage?,
    onDismiss: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(message?.id) {
        val id = message?.id ?: return@LaunchedEffect
        delay(BANNER_MS)
        onDismiss(id)
    }
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        Text(
            text = message?.text.orEmpty(),
            style = ContinueTextStyles.label,
            color = ContinueColors.SurfaceVoid,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = ContinueSpacing.LG.dp,
                    end = ContinueSpacing.LG.dp,
                    // Clears the raised DRAW button the same way the lists below it do.
                    bottom = ContinueSpacing.XXL.dp,
                )
                .clip(RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp))
                .background(ContinueColors.AccentNeon)
                .padding(ContinueSpacing.MD.dp),
        )
    }
}

/** Where a game goes when the plain + isn't what you meant. */
private val ADD_TARGETS: List<Triple<PileState, String, String>> = listOf(
    Triple(PileState.BACKLOG, "THE PILE", "Own it, haven't started"),
    Triple(PileState.WISHLIST, "WANTED", "Don't own it yet"),
    Triple(PileState.COMPLETED, "ALREADY CLEARED", "Log one you finished — pick the dates"),
)

/**
 * The long-press menu on a search result.
 *
 * DISCOVER could only ever add to THE PILE, so a game you'd already finished had to be added,
 * found again in PILE, moved to CLEARED, and then given a Credits Roll it didn't deserve. The
 * third option here is the entry point for backdating and the reason the feature is reachable
 * at all — a tester asked for exactly this.
 */
@Composable
private fun AddOptionsDialog(game: GameDto, onDismiss: () -> Unit, onPick: (PileState) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(game.name) },
        text = {
            Column {
                Text(text = "ADD TO", style = ContinueTextStyles.label, color = ContinueColors.TextTertiary)
                ADD_TARGETS.forEach { (target, label, description) ->
                    TextButton(onClick = { onPick(target) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = label, style = ContinueTextStyles.body, color = ContinueColors.AccentCoin)
                            Text(
                                text = description,
                                style = ContinueTextStyles.label,
                                color = ContinueColors.TextTertiary,
                            )
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
private fun SearchResultsList(
    results: List<GameDto>,
    isSearching: Boolean,
    note: String?,
    addedGameIds: Set<Long>,
    onAdd: (GameDto) -> Unit,
    onMore: (GameDto) -> Unit,
) {
    if (isSearching && results.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(ContinueSpacing.XL.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ContinueColors.AccentCoin)
        }
        return
    }
    if (results.isEmpty()) {
        Text(
            text = "NO MATCH FOUND",
            style = ContinueTextStyles.titleM,
            color = ContinueColors.TextSecondary,
            modifier = Modifier.padding(ContinueSpacing.XL.dp),
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(
            start = ContinueSpacing.LG.dp,
            end = ContinueSpacing.LG.dp,
            // The raised DRAW button overhangs the nav bar by 24dp; without clearance the last
            // row sits underneath it.
            bottom = ContinueSpacing.XXL.dp,
        ),
    ) {
        if (note != null) {
            item(key = "note") {
                Text(
                    text = note,
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextTertiary,
                    modifier = Modifier.padding(bottom = ContinueSpacing.SM.dp),
                )
            }
        }
        items(results, key = { it.id }) { game ->
            SearchResultRow(
                game = game,
                isAdded = game.id in addedGameIds,
                onAdd = { onAdd(game) },
                onMore = { onMore(game) },
            )
        }
    }
}

/**
 * One search hit: box art, name, and the year + length that tell two same-named games apart.
 *
 * Both details answer closed-test reports. The cover was asked for directly ("*if there could
 * be pictures next to game name on search bar*") and is most of what makes a list of titles
 * scannable at all. The **year** is the answer to "Blasphemous appears twice": IGDB carries
 * four different games called *Spider-Man*, and without a date they are four identical rows
 * and the app looks broken. True duplicates are removed upstream by
 * [com.mikhilnaika.continueapp.core.util.SearchRanking.dedupe]; what survives to here is
 * genuinely more than one game.
 */
@Composable
private fun SearchResultRow(game: GameDto, isAdded: Boolean, onAdd: () -> Unit, onMore: () -> Unit) {
    val meta = listOfNotNull(
        game.released?.take(4)?.takeIf { it.isNotBlank() },
        Playtime.estimateHours(
            game.playtimeHoursHastily,
            game.playtimeHoursNormally,
            game.playtimeHoursCompletely,
        )?.let { "$it HRS" },
        game.platforms.firstOrNull(),
    ).joinToString("  ·  ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ContinueSpacing.SM.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(6.dp))
                .background(ContinueColors.SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            // The initial sits underneath, so a row whose art hasn't arrived still reads as a
            // game rather than as an empty slab — the same rule GameCard follows.
            Text(
                text = game.name.take(1).uppercase(),
                style = ContinueTextStyles.titleM,
                color = ContinueColors.TextTertiary,
            )
            if (game.coverUrl != null) {
                AsyncImage(
                    model = IgdbImage.at(game.coverUrl, IgdbImage.GRID),
                    contentDescription = game.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = ContinueSpacing.MD.dp)
                .clickable(onClick = onMore),
        ) {
            Text(
                text = game.name,
                style = ContinueTextStyles.body,
                color = ContinueColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Still enabled when already added: a disabled button that does nothing on tap is how a
        // user ends up unsure whether the add ever worked. Tapping it says so instead.
        IconButton(onClick = onAdd) {
            Icon(
                imageVector = if (isAdded) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = if (isAdded) "Already in your pile" else "Add to pile",
                tint = if (isAdded) ContinueColors.AccentNeon else ContinueColors.AccentCoin,
            )
        }
    }
}

@Composable
private fun Rails(
    rails: List<DiscoverRail>,
    addedGameIds: Set<Long>,
    onAdd: (GameDto) -> Unit,
    onMore: (GameDto) -> Unit,
    onShowMore: (String) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = ContinueSpacing.XXL.dp)) {
        item(key = "share-hint") { ShareHint() }
        items(rails, key = { it.key }) { rail ->
            Rail(
                rail = rail,
                addedGameIds = addedGameIds,
                onAdd = onAdd,
                onMore = onMore,
                onShowMore = { onShowMore(rail.key) },
            )
        }
        item(key = "igdb") { IgdbAttribution() }
    }
}

/**
 * The one place DISCOVER admits it isn't the main way to find games.
 *
 * The app's actual discovery mechanism is the Android share target — you see a game in a video
 * and share it straight into the pile — and until now *nothing in the app said so anywhere*:
 * not onboarding, not any empty state. A closed-test tester duly spent their time on this
 * screen, judged it thin, and asked for an endless catalogue to browse. That's the wrong fix
 * for the right complaint: the feature they needed was one they had no way to discover.
 */
@Composable
private fun ShareHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ContinueSpacing.LG.dp)
            .clip(RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp))
            .background(ContinueColors.SurfaceCabinet)
            .padding(ContinueSpacing.MD.dp),
    ) {
        Text(
            text = "SAW A GAME IN A VIDEO?",
            style = ContinueTextStyles.label,
            color = ContinueColors.AccentCoin,
        )
        Text(
            text = "Hit share in TikTok, YouTube or Reddit and pick CONTINUE? — " +
                "it reads the title and drops the game straight into your pile.",
            style = ContinueTextStyles.body,
            color = ContinueColors.TextSecondary,
        )
    }
}

/**
 * Required, not decorative: IGDB's commercial-partnership terms ask for user-facing
 * attribution on products integrating their data, and CLAUDE.md makes it non-negotiable on
 * any screen showing it. DISCOVER is the most IGDB-dense screen in the app — search results,
 * both rails, and all the cover art come straight from them.
 */
@Composable
private fun IgdbAttribution() {
    Text(
        text = "The data was freely provided by IGDB.com",
        style = ContinueTextStyles.label,
        color = ContinueColors.TextTertiary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(ContinueSpacing.LG.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Rail(
    rail: DiscoverRail,
    addedGameIds: Set<Long>,
    onAdd: (GameDto) -> Unit,
    onMore: (GameDto) -> Unit,
    onShowMore: () -> Unit,
) {
    if (rail.games.isEmpty()) return
    Column(modifier = Modifier.padding(vertical = ContinueSpacing.SM.dp)) {
        Text(
            text = rail.title,
            style = ContinueTextStyles.label,
            color = ContinueColors.TextSecondary,
            modifier = Modifier.padding(horizontal = ContinueSpacing.LG.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = ContinueSpacing.LG.dp),
            horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
        ) {
            items(rail.games, key = { it.id }) { game ->
                Box(modifier = Modifier.width(120.dp)) {
                    GameCard(
                        title = game.name,
                        coverUrl = game.coverUrl,
                        onClick = { onAdd(game) },
                        onLongClick = { onMore(game) },
                    )
                    if (game.id in addedGameIds) {
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(ContinueColors.AccentNeon),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Added",
                                tint = ContinueColors.SurfaceVoid,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
            // Lives at the end of the rail rather than beside its heading, so it's reached by
            // the same gesture that got you there — you scroll to the end and it's waiting.
            if (rail.canLoadMore) {
                item(key = "more") {
                    ShowMoreCard(isLoading = rail.isLoadingMore, onClick = onShowMore)
                }
            }
        }
    }
}

@Composable
private fun ShowMoreCard(isLoading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(120.dp)
            // Matches GameCard's 3:4 art plus its two-line caption, so the rail's baseline
            // doesn't jump where the button sits.
            .height(220.dp)
            .clip(RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp))
            .background(ContinueColors.SurfaceCabinet)
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = ContinueColors.AccentCoin, modifier = Modifier.size(24.dp))
        } else {
            Text(
                text = "SHOW\nMORE ▸",
                style = ContinueTextStyles.label,
                color = ContinueColors.AccentCoin,
                textAlign = TextAlign.Center,
            )
        }
    }
}
