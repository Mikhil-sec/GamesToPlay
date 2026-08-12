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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.network.dto.GameDto
import com.mikhilnaika.continueapp.core.ui.GameCard

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(ContinueSpacing.LG.dp),
            placeholder = { Text("SEARCH FOR A GAME") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
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
                trending = state.trending,
                shortAndSweet = state.shortAndSweet,
                addedGameIds = state.addedGameIds,
                onAdd = viewModel::addToPile,
            )
        }
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
    LazyColumn(contentPadding = PaddingValues(horizontal = ContinueSpacing.LG.dp)) {
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
        IconButton(onClick = onAdd, enabled = !isAdded) {
            Icon(
                imageVector = if (isAdded) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = if (isAdded) "Added" else "Add to pile",
                tint = if (isAdded) ContinueColors.AccentNeon else ContinueColors.AccentCoin,
            )
        }
    }
}

@Composable
private fun Rails(
    trending: List<GameDto>,
    shortAndSweet: List<GameDto>,
    addedGameIds: Set<Long>,
    onAdd: (GameDto) -> Unit,
) {
    LazyColumn {
        item { Rail(title = "TRENDING NOW", games = trending, addedGameIds = addedGameIds, onAdd = onAdd) }
        item { Rail(title = "SHORT & SWEET", games = shortAndSweet, addedGameIds = addedGameIds, onAdd = onAdd) }
        item { IgdbAttribution() }
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
