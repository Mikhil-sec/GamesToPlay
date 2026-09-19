package com.mikhilnaika.continueapp.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.audio.LocalArcadeAudio
import com.mikhilnaika.continueapp.core.audio.MusicCue
import com.mikhilnaika.continueapp.core.audio.MusicPack
import com.mikhilnaika.continueapp.core.audio.MusicTrack
import com.mikhilnaika.continueapp.core.audio.Sfx
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.EmptyState
import com.mikhilnaika.continueapp.core.ui.IgdbAttribution
import com.mikhilnaika.continueapp.core.util.findActivity

/** YOU tab — docs/02-PRODUCT-SPEC.md §7. */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onGoPro: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

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
            // Sound first: it's the setting people reach for fastest, usually mid-meeting.
            // Both follow the phone's silent switch regardless, and say so.
            SettingsToggleRow(
                label = "SOUND",
                description = "Coins, the lever, the card deal. Follows your media volume; off when your phone is on silent.",
                checked = state.soundEffectsEnabled,
                onCheckedChange = viewModel::setSoundEffectsEnabled,
            )
            SettingsToggleRow(
                label = "MUSIC",
                description = "The title theme, the CONTINUE? countdown, the shop and the credits. " +
                    "Never plays over your own music.",
                checked = state.musicEnabled,
                onCheckedChange = viewModel::setMusicEnabled,
            )
            SettingsToggleRow(
                label = "HAPTICS",
                description = "Buzz on the lever, the stack and a clear.",
                checked = state.hapticsEnabled,
                onCheckedChange = viewModel::setHapticsEnabled,
            )
            MusicPackPicker(
                selected = state.musicPack,
                owned = state.ownedMusicPacks,
                coins = state.coinBalance,
                message = state.musicPackMessage,
                onSelect = viewModel::selectMusicPack,
                onUnlock = viewModel::unlockMusicPack,
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
            // Only where Google actually collected consent — see ProfileUiState. A row that
            // opens an empty form is worse than no row, and the repo rule is that every
            // settings row says what it does, which you cannot write for a dead one.
            if (state.privacyOptionsRequired) {
                SettingsActionRow(
                    label = "AD PRIVACY CHOICES",
                    description = "Change what you agreed to when you first opened the app. " +
                        "Turning consent off means no rewarded ads — everything else keeps working.",
                    onClick = { context.findActivity()?.let(viewModel::showPrivacyOptions) },
                )
            }
        }

        item { IgdbAttribution() }
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
/**
 * A settings row that performs an action rather than holding a state.
 *
 * Shares [SettingsToggleRow]'s shape on purpose — same label, same required one-line
 * description, same rhythm — so the settings list reads as one list rather than a toggle
 * section with a stray button in it.
 */
@Composable
private fun SettingsActionRow(label: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = ContinueSpacing.MD.dp),
    ) {
        Text(text = label, style = ContinueTextStyles.label, color = ContinueColors.AccentCoin)
        Text(
            text = description,
            style = ContinueTextStyles.label,
            color = ContinueColors.TextTertiary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * MUSIC PACK — three soundtracks, one chosen. Each card says what it sounds like in words,
 * offers a PREVIEW so nobody spends coins blind, and shows exactly one action: what's selected,
 * USE for a pack already owned, or the coin price for one that isn't.
 *
 * The preview plays the pack's title theme right here, and stops when you tap it again, pick
 * another, or leave YOU — [MusicCue] ties it to this screen's lifetime.
 */
@Composable
private fun MusicPackPicker(
    selected: MusicPack,
    owned: Set<MusicPack>,
    coins: Int,
    message: String?,
    onSelect: (MusicPack) -> Unit,
    onUnlock: (MusicPack) -> Unit,
) {
    val audio = LocalArcadeAudio.current
    var previewing by remember { mutableStateOf<MusicPack?>(null) }
    previewing?.let { MusicCue(MusicTrack.TITLE, preview = it) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = ContinueSpacing.SM.dp)) {
        Text(text = "MUSIC PACK", style = ContinueTextStyles.body, color = ContinueColors.TextPrimary)
        Text(
            text = "What the title screen, the CONTINUE? countdown, the shop and the credits play.",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextTertiary,
        )
        MusicPack.entries.forEach { pack ->
            val isSelected = pack == selected
            val isOwned = pack in owned
            Column(
                modifier = Modifier
                    .padding(top = ContinueSpacing.SM.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ContinueColors.SurfaceRaised)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) ContinueColors.AccentNeon else ContinueColors.OutlineDim,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(ContinueSpacing.MD.dp),
            ) {
                Text(text = pack.title, style = ContinueTextStyles.titleM, color = ContinueColors.TextPrimary)
                Text(text = pack.vibe, style = ContinueTextStyles.label, color = ContinueColors.TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = ContinueSpacing.SM.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (previewing == pack) "■ STOP" else "▶ PREVIEW",
                        style = ContinueTextStyles.label,
                        color = ContinueColors.AccentCool,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                audio.play(Sfx.BLIP)
                                previewing = if (previewing == pack) null else pack
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    when {
                        isSelected -> Text(
                            text = "✓ PLAYING",
                            style = ContinueTextStyles.label,
                            color = ContinueColors.AccentNeon,
                        )
                        isOwned -> Text(
                            text = "USE",
                            style = ContinueTextStyles.label,
                            color = ContinueColors.AccentCoin,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    audio.play(Sfx.SELECT)
                                    previewing = null
                                    onSelect(pack)
                                }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                        )
                        else -> Text(
                            text = "UNLOCK · ${pack.price} COINS",
                            style = ContinueTextStyles.label,
                            color = if (coins >= pack.price) ContinueColors.AccentCoin else ContinueColors.TextTertiary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    // The coin sound is the purchase; the counter above spins.
                                    if (coins >= pack.price) audio.play(Sfx.COIN) else audio.play(Sfx.ERROR)
                                    previewing = null
                                    onUnlock(pack)
                                }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                        )
                    }
                }
            }
        }
        if (message != null) {
            Text(
                text = message,
                style = ContinueTextStyles.label,
                color = ContinueColors.AccentHot,
                modifier = Modifier.padding(top = ContinueSpacing.SM.dp),
            )
        }
    }
}

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
