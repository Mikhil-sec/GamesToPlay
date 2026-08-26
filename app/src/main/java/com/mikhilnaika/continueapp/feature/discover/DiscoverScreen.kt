package com.mikhilnaika.continueapp.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mikhilnaika.continueapp.core.design.ContinueShapes
import com.mikhilnaika.continueapp.core.network.dto.GameDto
import com.mikhilnaika.continueapp.core.ui.GameCard

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

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
                    addedGameIds = state.addedGameIds,
                    onAdd = viewModel::addToPile,
                )
            } else {
                Rails(
                    rails = state.rails,
                    addedGameIds = state.addedGameIds,
                    onAdd = viewModel::addToPile,
                )
            }
        }

        AddedBanner(
            message = state.message,
            onDismiss = viewModel::consumeMessage,
            modifier = Modifier.align(Alignment.BottomCenter),
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

@Composable
private fun SearchResultsList(
    results: List<GameDto>,
    isSearching: Boolean,
    addedGameIds: Set<Long>,
    onAdd: (GameDto) -> Unit,
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
        items(results, key = { it.id }) { game ->
            SearchResultRow(game = game, isAdded = game.id in addedGameIds, onAdd = { onAdd(game) })
        }
    }
}

@Composable
private fun SearchResultRow(game: GameDto, isAdded: Boolean, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ContinueSpacing.SM.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = game.name,
            style = ContinueTextStyles.body,
            color = ContinueColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
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
) {
    LazyColumn(contentPadding = PaddingValues(bottom = ContinueSpacing.XXL.dp)) {
        item(key = "share-hint") { ShareHint() }
        items(rails, key = { it.key }) { rail ->
            Rail(title = rail.title, games = rail.games, addedGameIds = addedGameIds, onAdd = onAdd)
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
    title: String,
    games: List<GameDto>,
    addedGameIds: Set<Long>,
    onAdd: (GameDto) -> Unit,
) {
    if (games.isEmpty()) return
    Column(modifier = Modifier.padding(vertical = ContinueSpacing.SM.dp)) {
        Text(
            text = title,
            style = ContinueTextStyles.label,
            color = ContinueColors.TextSecondary,
            modifier = Modifier.padding(horizontal = ContinueSpacing.LG.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = ContinueSpacing.LG.dp),
            horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
        ) {
            items(games, key = { it.id }) { game ->
                Box(modifier = Modifier.width(120.dp)) {
                    GameCard(
                        title = game.name,
                        coverUrl = game.coverUrl,
                        onClick = { onAdd(game) },
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
        }
    }
}
