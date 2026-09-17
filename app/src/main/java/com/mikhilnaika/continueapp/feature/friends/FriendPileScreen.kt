package com.mikhilnaika.continueapp.feature.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.data.PileState
import com.mikhilnaika.continueapp.core.data.dao.FriendGameRow
import com.mikhilnaika.continueapp.core.data.dao.FriendRankRow
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.ArcadeButton
import com.mikhilnaika.continueapp.core.ui.EmptyState
import com.mikhilnaika.continueapp.core.ui.GameCard
import com.mikhilnaika.continueapp.core.ui.IgdbAttribution
import com.mikhilnaika.continueapp.core.util.IgdbImage
import com.mikhilnaika.continueapp.core.util.Playtime
import com.mikhilnaika.continueapp.core.util.RelativeTime

/** A game picked on a friend's pile, for the add-to-mine dialog. */
private data class PickedGame(val gameId: Long, val name: String, val coverUrl: String?, val where: String, val inMyPile: Boolean)

/**
 * One friend's pile, read-only — docs/02-PRODUCT-SPEC.md §6. Same tabs as yours, plus their
 * HIGH SCORES, with the games you both have marked so the overlap is the first thing you see.
 */
@Composable
fun FriendPileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendPileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val message by viewModel.message.collectAsState()
    var picked by remember { mutableStateOf<PickedGame?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }

    val friend = state.friend
    if (state.isLoaded && friend == null) {
        // Removed — REMOVE below, or a stale deep link. This is the only place that navigates
        // away for it; the remove action itself doesn't, or Back would run twice.
        LaunchedEffect(Unit) { onBack() }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = ContinueSpacing.LG.dp,
                end = ContinueSpacing.LG.dp,
                top = ContinueSpacing.SM.dp,
                bottom = ContinueSpacing.XXL.dp + ContinueSpacing.XL.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
            verticalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ContinueColors.TextPrimary)
                        }
                        if (friend != null) {
                            FriendAvatar(friend.name, size = 40.dp)
                            Column(modifier = Modifier.weight(1f).padding(horizontal = ContinueSpacing.MD.dp)) {
                                Text(
                                    text = friend.name,
                                    style = ContinueTextStyles.titleL,
                                    color = ContinueColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Shared ${RelativeTime.label(friend.sharedAt, System.currentTimeMillis())}",
                                    style = ContinueTextStyles.label,
                                    color = ContinueColors.TextTertiary,
                                )
                            }
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "Friend options", tint = ContinueColors.TextSecondary)
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; renaming = true })
                                    DropdownMenuItem(
                                        text = { Text("Remove", color = ContinueColors.AccentHot) },
                                        onClick = { menuOpen = false; removing = true },
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }

                    if (friend != null) {
                        Spacer(Modifier.height(ContinueSpacing.SM.dp))
                        PileStatRow(
                            listOf(
                                friend.backlogTotal to "PILE",
                                friend.playingTotal to "PLAYING",
                                friend.clearedTotal to "CLEARED",
                                state.inCommon to "IN COMMON",
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = ContinueSpacing.SM.dp),
                        horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
                    ) {
                        FriendTab.entries.forEach { tab ->
                            FilterChip(
                                selected = state.tab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                label = { Text("${tab.label}  ${state.counts[tab] ?: 0}") },
                            )
                        }
                    }
                }
            }

            when {
                !state.isLoaded -> item(span = { GridItemSpan(maxLineSpan) }, key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(ContinueSpacing.XXL.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ContinueColors.AccentCoin)
                    }
                }

                state.tab == FriendTab.TOP -> {
                    if (state.ranks.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "empty-top") {
                            EmptyState(
                                headline = "NO HIGH SCORES YET",
                                supporting = "${friend?.name ?: "They"} hasn't ranked anything.",
                                modifier = Modifier.fillMaxWidth().height(220.dp),
                            )
                        }
                    } else {
                        items(state.ranks, key = { "rank-${it.position}" }, span = { GridItemSpan(maxLineSpan) }) { rank ->
                            RankRow(rank, isHydrating = state.isHydrating) {
                                picked = PickedGame(
                                    gameId = rank.gameId,
                                    name = rank.name ?: return@RankRow,
                                    coverUrl = rank.coverUrl,
                                    where = "#${rank.position} on ${friend?.name}'s high scores",
                                    inMyPile = rank.inMyPile,
                                )
                            }
                        }
                    }
                }

                state.games.isEmpty() && state.notInLink == 0 -> item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                    EmptyState(
                        headline = "NOTHING HERE",
                        supporting = "${friend?.name ?: "They"} has no games in ${state.tab.label}.",
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                    )
                }

                else -> {
                    items(state.games, key = { it.gameId }) { game ->
                        FriendGameCard(game, isHydrating = state.isHydrating) {
                            picked = PickedGame(
                                gameId = game.gameId,
                                name = game.name ?: return@FriendGameCard,
                                coverUrl = game.coverUrl,
                                where = "In ${friend?.name}'s ${stateLabel(game.state)}",
                                inMyPile = game.inMyPile,
                            )
                        }
                    }
                    if (state.notInLink > 0) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "more") {
                            Text(
                                text = "+${state.notInLink} more that didn't fit in their link",
                                style = ContinueTextStyles.label,
                                color = ContinueColors.TextTertiary,
                                modifier = Modifier.padding(vertical = ContinueSpacing.SM.dp),
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "igdb") { IgdbAttribution() }
        }

        if (message != null) {
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(2_000)
                viewModel.messageShown()
            }
            Toast(message.orEmpty(), Modifier.align(Alignment.BottomCenter).padding(bottom = ContinueSpacing.XXL.dp))
        }
    }

    picked?.let { game ->
        AlertDialog(
            onDismissRequest = { picked = null },
            title = { Text(game.name) },
            text = {
                Column {
                    Text(game.where, style = ContinueTextStyles.body, color = ContinueColors.TextSecondary)
                    if (game.inMyPile) {
                        Spacer(Modifier.height(ContinueSpacing.SM.dp))
                        Text("You have this one too.", style = ContinueTextStyles.body, color = ContinueColors.AccentNeon)
                    }
                }
            },
            confirmButton = {
                if (!game.inMyPile) {
                    ArcadeButton(
                        text = "ADD TO MY PILE",
                        onClick = {
                            viewModel.addToMyPile(game.gameId, game.name, game.coverUrl)
                            picked = null
                        },
                    )
                }
            },
            dismissButton = { TextButton(onClick = { picked = null }) { Text(if (game.inMyPile) "CLOSE" else "CANCEL") } },
        )
    }

    if (renaming && friend != null) {
        RenameDialog(
            current = friend.name,
            onRename = { viewModel.rename(it); renaming = false },
            onDismiss = { renaming = false },
        )
    }

    if (removing && friend != null) {
        RemoveFriendDialog(
            name = friend.name,
            onRemove = { removing = false; viewModel.remove() },
            onDismiss = { removing = false },
        )
    }
}

@Composable
private fun FriendGameCard(game: FriendGameRow, isHydrating: Boolean, onClick: () -> Unit) {
    val name = game.name
    if (name == null) {
        UnnamedCard(isHydrating)
        return
    }
    GameCard(
        title = name,
        coverUrl = game.coverUrl,
        subtitle = if (game.inMyPile) {
            "YOU HAVE IT"
        } else {
            Playtime.label(game.playtimeHoursHastily, game.playtimeHoursNormally, game.playtimeHoursCompletely)
        },
        onClick = onClick,
    )
}

/** A game the friend has that this device can't name yet — kept visible so counts add up. */
@Composable
private fun UnnamedCard(isHydrating: Boolean) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ContinueColors.SurfaceCabinet),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)) { UnknownCover() }
        Text(
            text = if (isHydrating) "LOADING…" else "NOT LOADED YET",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextTertiary,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
private fun RankRow(rank: FriendRankRow, isHydrating: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ContinueColors.SurfaceCabinet)
            .clickable(enabled = rank.name != null, onClick = onClick)
            .padding(ContinueSpacing.SM.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ordinal(rank.position),
            style = ContinueTextStyles.monoL,
            color = when (rank.position) {
                1 -> ContinueColors.AccentCoin
                2 -> ContinueColors.TextPrimary
                3 -> ContinueColors.AccentHot
                else -> ContinueColors.TextSecondary
            },
            modifier = Modifier.width(56.dp),
        )
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 48.dp)
                .clip(RoundedCornerShape(6.dp)),
        ) {
            if (rank.coverUrl != null) {
                AsyncImage(
                    model = IgdbImage.at(rank.coverUrl, IgdbImage.GRID),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                UnknownCover()
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = ContinueSpacing.MD.dp)) {
            Text(
                text = rank.name ?: if (isHydrating) "Loading…" else "Not loaded yet",
                style = ContinueTextStyles.body,
                color = if (rank.name != null) ContinueColors.TextPrimary else ContinueColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (rank.inMyPile) {
                Text(text = "YOU HAVE IT", style = ContinueTextStyles.label, color = ContinueColors.AccentNeon)
            }
        }
    }
}

private fun ordinal(position: Int): String {
    val suffix = if (position % 100 in 11..13) "TH" else when (position % 10) {
        1 -> "ST"
        2 -> "ND"
        3 -> "RD"
        else -> "TH"
    }
    return "$position$suffix"
}

private fun stateLabel(state: PileState): String = when (state) {
    PileState.BACKLOG -> "pile"
    PileState.PLAYING -> "now playing"
    PileState.COMPLETED -> "cleared games"
    PileState.DROPPED -> "retired games"
    PileState.WISHLIST -> "wanted list"
}
