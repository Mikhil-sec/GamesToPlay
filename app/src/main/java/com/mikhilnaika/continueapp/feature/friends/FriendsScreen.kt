package com.mikhilnaika.continueapp.feature.friends

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikhilnaika.continueapp.core.data.entity.FriendEntity
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueShapes
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.friends.FriendRepository
import com.mikhilnaika.continueapp.core.ui.ArcadeButton
import com.mikhilnaika.continueapp.core.ui.EmptyState
import com.mikhilnaika.continueapp.core.ui.LocalHaptics
import com.mikhilnaika.continueapp.core.util.RelativeTime

/**
 * FRIENDS — every pile a friend has shared with you. docs/02-PRODUCT-SPEC.md §6.
 *
 * Deliberately not live: there is no server holding anyone's pile. Each row is the last snapshot
 * that friend sent, stamped with when they sent it, and it catches up when they share again.
 * The screen says that in as many words rather than letting "live" be assumed.
 */
@Composable
fun FriendsScreen(
    onOpenFriend: (Long) -> Unit,
    onSharePile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val message by viewModel.message.collectAsState()
    var actionFriend by remember { mutableStateOf<FriendEntity?>(null) }
    var renaming by remember { mutableStateOf<FriendEntity?>(null) }
    var removing by remember { mutableStateOf<FriendEntity?>(null) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHaptics.current

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = ContinueSpacing.LG.dp,
                end = ContinueSpacing.LG.dp,
                top = ContinueSpacing.LG.dp,
                // The raised DRAW button overhangs the nav bar.
                bottom = ContinueSpacing.XXL.dp + ContinueSpacing.XL.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
        ) {
            item(key = "title") {
                Column {
                    Text(text = "FRIENDS", style = ContinueTextStyles.displayL, color = ContinueColors.TextPrimary)
                    Text(
                        text = "Piles your friends have shared with you.",
                        style = ContinueTextStyles.body,
                        color = ContinueColors.TextSecondary,
                    )
                }
            }
            item(key = "share") { SharePrompt(onSharePile) }

            when {
                !state.isLoaded -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(ContinueSpacing.XXL.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ContinueColors.AccentCoin)
                    }
                }

                state.friends.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        headline = "NO PLAYERS ON THE BOARD",
                        supporting = "When a friend shares their pile from CONTINUE?, tap their link " +
                            "and they'll appear here with everything they're playing.",
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                    )
                }

                else -> {
                    item(key = "count") {
                        Text(
                            text = "${state.friends.size} FOLLOWING",
                            style = ContinueTextStyles.label,
                            color = ContinueColors.TextTertiary,
                            modifier = Modifier.padding(top = ContinueSpacing.SM.dp),
                        )
                    }
                    items(state.friends, key = { it.friend.id }) { row ->
                        FriendRow(
                            row = row,
                            onClick = { onOpenFriend(row.friend.id) },
                            onLongClick = {
                                haptics.light()
                                actionFriend = row.friend
                            },
                        )
                    }
                    item(key = "hint") {
                        Text(
                            text = "Piles don't update on their own — each one is the last version " +
                                "that friend sent. Long-press a friend to rename or remove them.",
                            style = ContinueTextStyles.label,
                            color = ContinueColors.TextTertiary,
                            modifier = Modifier.padding(top = ContinueSpacing.SM.dp),
                        )
                    }
                }
            }

            item(key = "reset") {
                TextButton(onClick = { confirmReset = true }) {
                    Text("Start a fresh share link", color = ContinueColors.TextTertiary)
                }
            }
        }

        if (message != null) {
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(2_000)
                viewModel.messageShown()
            }
            Toast(message.orEmpty(), Modifier.align(Alignment.BottomCenter).padding(bottom = ContinueSpacing.XXL.dp))
        }
    }

    actionFriend?.let { friend ->
        AlertDialog(
            onDismissRequest = { actionFriend = null },
            title = { Text(friend.name) },
            text = {
                Column {
                    TextButton(onClick = { renaming = friend; actionFriend = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("RENAME")
                    }
                    TextButton(onClick = { removing = friend; actionFriend = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("REMOVE", color = ContinueColors.AccentHot)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { actionFriend = null }) { Text("CANCEL") } },
        )
    }

    renaming?.let { friend ->
        RenameDialog(
            current = friend.name,
            onRename = { viewModel.rename(friend.id, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }

    removing?.let { friend ->
        RemoveFriendDialog(
            name = friend.name,
            onRemove = { viewModel.remove(friend); removing = null },
            onDismiss = { removing = null },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("START A FRESH SHARE LINK?") },
            text = {
                Text(
                    "Your next shared pile will be signed with a new key. Friends who follow your " +
                        "old one keep the last copy you sent, and it can't be updated or connected " +
                        "to the new one. Use this if a link went somewhere you didn't mean it to."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.resetMyLink(); confirmReset = false }) { Text("START FRESH") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("CANCEL") } },
        )
    }
}

@Composable
private fun SharePrompt(onSharePile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ContinueSpacing.SM.dp)
            .clip(RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp))
            .background(ContinueColors.SurfaceFelt)
            .border(1.dp, ContinueColors.AccentNeon.copy(alpha = 0.35f), RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp))
            .padding(ContinueSpacing.MD.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "YOUR TURN", style = ContinueTextStyles.label, color = ContinueColors.AccentNeon)
            Text(
                text = "Share your pile so friends can follow it.",
                style = ContinueTextStyles.body,
                color = ContinueColors.TextPrimary,
            )
        }
        Spacer(Modifier.width(ContinueSpacing.SM.dp))
        ArcadeButton(text = "SHARE", onClick = onSharePile, accent = ContinueColors.AccentNeon)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FriendRow(row: FriendRowUi, onClick: () -> Unit, onLongClick: () -> Unit) {
    val friend = row.friend
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp))
            .background(ContinueColors.SurfaceCabinet)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Rename or remove")
            .padding(ContinueSpacing.MD.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FriendAvatar(friend.name)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = ContinueSpacing.MD.dp),
        ) {
            Text(
                text = friend.name,
                style = ContinueTextStyles.titleM,
                color = ContinueColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${friend.backlogTotal} PILE · ${friend.playingTotal} PLAYING · ${friend.clearedTotal} CLEARED",
                style = ContinueTextStyles.label,
                color = ContinueColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Shared ${RelativeTime.label(friend.sharedAt, System.currentTimeMillis())}",
                style = ContinueTextStyles.label,
                color = ContinueColors.TextTertiary,
            )
        }
        CoverStrip(covers = row.covers)
    }
}

@Composable
internal fun RenameDialog(current: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("RENAME") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(FriendRepository.MAX_NAME_LENGTH) },
                singleLine = true,
                label = { Text("Name") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(name) }, enabled = FriendRepository.cleanName(name) != null) { Text("SAVE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}

@Composable
internal fun RemoveFriendDialog(name: String, onRemove: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("REMOVE $name?".uppercase()) },
        text = { Text("Their pile is deleted from this phone. If they share it again, you can add them back.") },
        confirmButton = { TextButton(onClick = onRemove) { Text("REMOVE", color = ContinueColors.AccentHot) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}

/** A small self-dismissing confirmation, drawn above the nav bar. */
@Composable
internal fun Toast(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = ContinueTextStyles.body,
        color = ContinueColors.SurfaceVoid,
        modifier = modifier
            .padding(horizontal = ContinueSpacing.LG.dp)
            .clip(RoundedCornerShape(ContinueShapes.RADIUS_BUTTON_DP.dp))
            .background(ContinueColors.AccentNeon)
            .padding(horizontal = ContinueSpacing.LG.dp, vertical = ContinueSpacing.MD.dp),
    )
}
