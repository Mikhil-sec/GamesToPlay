package com.mikhilnaika.continueapp.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.EmptyState

/** YOU tab — docs/02-PRODUCT-SPEC.md §7. */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onGoPro: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Reordering is a mode rather than always-on controls: three extra buttons per row would
    // turn a leaderboard into a toolbar, and the list is read far more often than it is edited.
    var editingScores by remember { mutableStateOf(false) }
    var showAllScores by remember { mutableStateOf(false) }
    var removingScore by remember { mutableStateOf<HighScoreEntry?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = ContinueSpacing.LG.dp),
        // Top: the coin counter sits above this screen, so it needs its own breathing room.
        // Bottom: the raised DRAW button overhangs the nav bar by 24dp.
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = ContinueSpacing.LG.dp,
            bottom = ContinueSpacing.XXL.dp,
        ),
    ) {
        item {
            Text(text = "YOU", style = ContinueTextStyles.displayL, color = ContinueColors.TextPrimary)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.LG.dp))
        }

        // Until now the only way to reach the paywall was to exhaust the daily free draw, which
        // means a judge (or a willing buyer) could easily never see it. YOU is where a user goes
        // to look at their own account, so it's the natural second door.
        if (!state.isPro) {
            item { GoProBanner(onGoPro = onGoPro) }
        }

        item { SectionHeader("THIS YEAR") }
        item { ThisYearGrid(state.thisYear) }
        item {
            // The second door into STATS. YOU is where a user comes to look at their own
            // numbers, and five tiles is where that stops being enough — but the header of PILE
            // has no width left for another icon (see PileHeader), so this is the discoverable
            // route rather than an extra one.
            TextButton(onClick = onOpenStats) {
                Text(
                    text = "▸ SEE THE FULL BREAKDOWN",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.AccentCoin,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionHeader("HIGH SCORES")
                if (state.highScores.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Hidden while editing: mid-reorder is exactly when the leaderboard is
                        // in a state its owner doesn't yet stand behind.
                        if (!editingScores) {
                            TextButton(
                                onClick = viewModel::shareHighScores,
                                enabled = !state.isPreparingShare,
                            ) {
                                Text(
                                    text = if (state.isPreparingShare) "…" else "SHARE",
                                    style = ContinueTextStyles.label,
                                    color = ContinueColors.AccentNeon,
                                )
                            }
                        }
                        TextButton(onClick = { editingScores = !editingScores }) {
                            Text(
                                text = if (editingScores) "DONE" else "EDIT",
                                style = ContinueTextStyles.label,
                                color = ContinueColors.AccentCoin,
                            )
                        }
                    }
                }
            }
        }
        item {
            if (state.highScores.isEmpty()) {
                EmptyState(headline = "RANK A GAME TO START YOUR LEADERBOARD")
            } else {
                val shown = if (showAllScores) state.highScores else state.highScores.take(10)
                Column {
                    shown.forEach { entry ->
                        HighScoreRow(
                            entry = entry,
                            editing = editingScores,
                            isFirst = entry.position == 1,
                            isLast = entry.position == state.highScores.size,
                            onMoveUp = { viewModel.moveHighScore(entry.gameId, -1) },
                            onMoveDown = { viewModel.moveHighScore(entry.gameId, 1) },
                            onRemove = { removingScore = entry },
                        )
                    }
                    if (state.highScores.size > 10) {
                        TextButton(onClick = { showAllScores = !showAllScores }) {
                            Text(
                                text = if (showAllScores) {
                                    "SHOW TOP 10"
                                } else {
                                    "SHOW ALL " + state.highScores.size
                                },
                                style = ContinueTextStyles.label,
                                color = ContinueColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeader("TROPHIES") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
                items(state.trophies, key = { it.id }) { TrophyChip(it) }
            }
        }

        item { SectionHeader("SETTINGS") }
        item {
            SettingsToggleRow(
                label = "HAPTICS",
                description = "Buzz on the lever, the stack and a clear.",
                checked = state.hapticsEnabled,
                onCheckedChange = viewModel::setHapticsEnabled,
            )
            SettingsToggleRow(
                label = "CLIPBOARD DETECTION",
                // Says what it reads and what the OS will do about it. Android 12 and up show a
                // "pasted from your clipboard" toast every time an app reads content it didn't
                // write, and a user who meets that toast without warning reasonably assumes the
                // app is snooping.
                description = "Copy a game's name anywhere and CONTINUE? offers to add it when " +
                    "you next open the app. Android will say the app read your clipboard — " +
                    "that's this. Nothing leaves your phone.",
                checked = state.clipboardDetectionEnabled,
                onCheckedChange = viewModel::setClipboardDetectionEnabled,
            )
        }

        item {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
            Text(
                text = "The data was freely provided by IGDB.com",
                style = ContinueTextStyles.label,
                color = ContinueColors.TextTertiary,
            )
        }
    }

    removingScore?.let { entry ->
        AlertDialog(
            onDismissRequest = { removingScore = null },
            title = { Text("DROP FROM HIGH SCORES?") },
            text = {
                Text(
                    // Spelled out because the two really are different, and the app already has
                    // a destructive REMOVE FROM PILE that this could easily be mistaken for.
                    text = entry.name + " keeps its place in CLEARED — this only takes it off " +
                        "the leaderboard. You can rank it again any time.",
                    style = ContinueTextStyles.body,
                    color = ContinueColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeHighScore(entry.gameId)
                    removingScore = null
                }) {
                    Text("DROP IT", color = ContinueColors.AccentHot)
                }
            },
            dismissButton = { TextButton(onClick = { removingScore = null }) { Text("KEEP IT") } },
            containerColor = ContinueColors.SurfaceRaised,
            textContentColor = ContinueColors.TextSecondary,
            titleContentColor = ContinueColors.TextPrimary,
        )
    }
}

@Composable
private fun GoProBanner(onGoPro: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ContinueColors.SurfaceRaised)
            .clickable(onClick = onGoPro)
            .padding(horizontal = ContinueSpacing.LG.dp, vertical = ContinueSpacing.MD.dp),
    ) {
        Text(text = "▸ GO PRO", style = ContinueTextStyles.titleM, color = ContinueColors.AccentCoin)
        Text(
            text = "Unlimited draws · unlimited stacks · no ads",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextSecondary,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = ContinueTextStyles.label,
        color = ContinueColors.TextSecondary,
        modifier = Modifier.padding(top = ContinueSpacing.LG.dp, bottom = ContinueSpacing.SM.dp),
    )
}

@Composable
private fun ThisYearGrid(stats: ThisYearStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
        StatTile("CLEARED", "${stats.gamesCleared}", modifier = Modifier.weight(1f))
        StatTile("HOURS", "${stats.hoursCleared}", modifier = Modifier.weight(1f))
        StatTile("STREAK", "${stats.currentStreakDays}D", modifier = Modifier.weight(1f))
    }
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
        StatTile("LONGEST CLEAR", "${stats.longestGameHours}H", modifier = Modifier.weight(1f))
        StatTile("FASTEST CLEAR", stats.fastestClearDays?.let { "${it}D" } ?: "—", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ContinueColors.SurfaceRaised)
            .padding(ContinueSpacing.MD.dp),
    ) {
        Text(text = value, style = ContinueTextStyles.monoL, color = ContinueColors.AccentCoin)
        Text(text = label, style = ContinueTextStyles.label, color = ContinueColors.TextSecondary)
    }
}

/**
 * One row of the leaderboard, with the manual override RANK never had.
 *
 * Up/down arrows rather than drag-to-reorder: a drag handle inside an already-scrolling
 * `LazyColumn` fights the scroll on a phone, and a leaderboard is corrected by a slot or two at
 * a time, not rearranged wholesale. `#1` can't move up and the last row can't move down, so the
 * buttons disable rather than silently doing nothing.
 */
@Composable
private fun HighScoreRow(
    entry: HighScoreEntry,
    editing: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = ContinueSpacing.XS.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#${entry.position}",
            style = ContinueTextStyles.monoL,
            color = ContinueColors.AccentCoin,
            modifier = Modifier.width(48.dp),
        )
        Box(
            modifier = Modifier
                .width(32.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(4.dp))
                .background(ContinueColors.SurfaceRaised),
        ) {
            if (entry.coverUrl != null) {
                AsyncImage(model = entry.coverUrl, contentDescription = entry.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = ContinueSpacing.SM.dp)) {
            Text(text = entry.name, style = ContinueTextStyles.body, color = ContinueColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            entry.verdict?.let {
                Text(text = it, style = ContinueTextStyles.label, color = ContinueColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (editing) {
            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Move up",
                    tint = if (isFirst) ContinueColors.TextTertiary else ContinueColors.TextSecondary,
                )
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Move down",
                    tint = if (isLast) ContinueColors.TextTertiary else ContinueColors.TextSecondary,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Drop from high scores",
                    tint = ContinueColors.AccentHot,
                )
            }
        }
    }
}

@Composable
private fun TrophyChip(trophy: Trophy) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (trophy.unlocked) ContinueColors.SurfaceRaised else ContinueColors.SurfaceCabinet)
            .padding(ContinueSpacing.SM.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (trophy.unlocked) ContinueColors.AccentCoin else ContinueColors.OutlineDim),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XS.dp))
        Text(
            text = trophy.label,
            style = ContinueTextStyles.label,
            color = if (trophy.unlocked) ContinueColors.TextPrimary else ContinueColors.TextTertiary,
            maxLines = 2,
        )
        Text(
            text = trophy.description,
            style = ContinueTextStyles.label,
            color = ContinueColors.TextTertiary,
            maxLines = 2,
        )
    }
}

/**
 * A settings row that says what the switch does.
 *
 * Both toggles in this list shipped with nothing but a shouty label, and both turned out to
 * control nothing at all — HAPTICS because every screen built its own engine, CLIPBOARD
 * DETECTION because no code outside this file had ever read it. A one-line description is the
 * cheapest guard against the next one: it is hard to write "what this does" for a switch that
 * does nothing.
 */
@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = ContinueSpacing.SM.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = ContinueSpacing.MD.dp)) {
            Text(text = label, style = ContinueTextStyles.body, color = ContinueColors.TextPrimary)
            if (description != null) {
                Text(
                    text = description,
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextTertiary,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
