package com.mikhilnaika.continueapp.feature.friends

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mikhilnaika.continueapp.MainActivity
import com.mikhilnaika.continueapp.core.data.entity.FriendEntity
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueShapes
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.design.ContinueTheme
import com.mikhilnaika.continueapp.core.design.enableArcadeEdgeToEdge
import com.mikhilnaika.continueapp.core.friends.FriendRepository
import com.mikhilnaika.continueapp.core.friends.PileDiff
import com.mikhilnaika.continueapp.core.share.ShareLinks
import com.mikhilnaika.continueapp.core.ui.ArcadeButton
import com.mikhilnaika.continueapp.core.util.RelativeTime
import dagger.hilt.android.AndroidEntryPoint

/**
 * Where a friend's pile link lands — a transparent sheet over whatever app it was tapped in,
 * the same shape as the share target, so following someone never means leaving the chat.
 *
 * A sheet rather than a MainActivity route for the same reason as the share target: the main
 * app may not have finished onboarding, and "someone sent you their pile" shouldn't have to
 * wait behind it.
 */
@AndroidEntryPoint
class FriendImportActivity : ComponentActivity() {

    private val viewModel: FriendImportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The scrim runs behind the system bars; the sheet itself pads for them below.
        enableArcadeEdgeToEdge()
        // Only VIEW intents carry a pile; anything else reads as an empty (invalid) link.
        val payload = if (intent.action == Intent.ACTION_VIEW) ShareLinks.parsePilePayload(intent.data) else null
        viewModel.open(payload)

        setContent {
            ContinueTheme {
                val state by viewModel.state.collectAsState()
                ImportSheet(
                    state = state,
                    onAdd = viewModel::addFriend,
                    onReplace = viewModel::replaceFriend,
                    onOpenFriend = ::openFriend,
                    onDismiss = ::finish,
                )
            }
        }
    }

    private fun openFriend(friendId: Long?) {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                // 0 means "the FRIENDS list" — used when the list is full and needs pruning.
                putExtra(MainActivity.EXTRA_OPEN_FRIEND_ID, friendId ?: 0L)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
        finish()
    }
}

@Composable
private fun ImportSheet(
    state: FriendImportState,
    onAdd: (String) -> Unit,
    onReplace: (FriendEntity) -> Unit,
    onOpenFriend: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            )
            // A tall sheet (a long candidate list, or the keyboard pushing it up) must stop at
            // the status bar rather than slide under the clock.
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = ContinueShapes.RADIUS_SHEET_TOP_DP.dp,
                        topEnd = ContinueShapes.RADIUS_SHEET_TOP_DP.dp,
                    )
                )
                .background(ContinueColors.SurfaceCabinet)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(ContinueSpacing.XL.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "FRIENDS",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.AccentNeon,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = ContinueColors.TextSecondary)
                }
            }

            when (state) {
                FriendImportState.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = ContinueColors.AccentCoin) }

                FriendImportState.Invalid -> Message(
                    title = "THIS LINK DIDN'T COME THROUGH WHOLE",
                    body = "Pile links are long, and some apps cut them short. Ask your friend to " +
                        "share their pile again, and open the link from the full message.",
                    primary = "DONE" to { onDismiss() },
                )

                FriendImportState.OwnPile -> Message(
                    title = "THAT'S YOUR PILE",
                    body = "Send this link to a friend with CONTINUE? and they can follow your pile " +
                        "from their FRIENDS tab.",
                    primary = "DONE" to { onDismiss() },
                )

                is FriendImportState.NewFriend -> NewFriendContent(state, onAdd, onReplace, onOpenFriend)

                is FriendImportState.Saved -> Message(
                    title = "${state.name.uppercase()} IS ON YOUR BOARD",
                    body = "Their pile is in FRIENDS. Each time they share it again, tap the new " +
                        "link and your copy catches up.",
                    primary = "VIEW THEIR PILE" to { onOpenFriend(state.friendId) },
                    secondary = "DONE" to { onDismiss() },
                    accent = ContinueColors.AccentNeon,
                )

                is FriendImportState.Updated -> Message(
                    title = "${state.name.uppercase()}'S PILE UPDATED",
                    body = diffLine(state.diff),
                    primary = "VIEW THEIR PILE" to { onOpenFriend(state.friendId) },
                    secondary = "DONE" to { onDismiss() },
                    accent = ContinueColors.AccentNeon,
                )

                is FriendImportState.AlreadyCurrent -> Message(
                    title = "ALREADY UP TO DATE",
                    body = "You have the latest version of ${state.name}'s pile.",
                    primary = "VIEW THEIR PILE" to { onOpenFriend(state.friendId) },
                    secondary = "DONE" to { onDismiss() },
                )

                is FriendImportState.Older -> Message(
                    title = "YOU HAVE A NEWER COPY",
                    body = "This link is older than the version of ${state.name}'s pile already on " +
                        "your phone, so nothing changed.",
                    primary = "VIEW THEIR PILE" to { onOpenFriend(state.friendId) },
                    secondary = "DONE" to { onDismiss() },
                )
            }
        }
    }
}

@Composable
private fun NewFriendContent(
    state: FriendImportState.NewFriend,
    onAdd: (String) -> Unit,
    onReplace: (FriendEntity) -> Unit,
    onOpenFriend: (Long?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var showExisting by rememberSaveable { mutableStateOf(false) }
    val preview = state.preview

    Text(text = "A FRIEND SHARED THEIR PILE", style = ContinueTextStyles.titleL, color = ContinueColors.TextPrimary)
    Spacer(Modifier.height(ContinueSpacing.XS.dp))
    Text(
        text = "Shared ${RelativeTime.label(preview.sharedAtMillis, System.currentTimeMillis())}",
        style = ContinueTextStyles.body,
        color = ContinueColors.TextSecondary,
    )

    if (preview.covers.isNotEmpty()) {
        Spacer(Modifier.height(ContinueSpacing.MD.dp))
        CoverStrip(covers = preview.covers, coverWidth = 52.dp)
    }

    Spacer(Modifier.height(ContinueSpacing.MD.dp))
    PileStatRow(
        listOf(
            preview.backlog to "PILE",
            preview.playing to "PLAYING",
            preview.cleared to "CLEARED",
        )
    )

    Spacer(Modifier.height(ContinueSpacing.LG.dp))

    if (state.atLimit) {
        Text(
            text = state.error
                ?: "You're following ${FriendRepository.MAX_FRIENDS} piles already — remove one in FRIENDS to add another.",
            style = ContinueTextStyles.body,
            color = ContinueColors.AccentHot,
        )
        Spacer(Modifier.height(ContinueSpacing.MD.dp))
        ArcadeButton(text = "OPEN FRIENDS", onClick = { onOpenFriend(null) }, modifier = Modifier.fillMaxWidth())
        return
    }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(FriendRepository.MAX_NAME_LENGTH) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("What do you call them?") },
        placeholder = { Text("e.g. Sam") },
        isError = state.error != null,
        supportingText = state.error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onAdd(name) }),
    )
    Spacer(Modifier.height(ContinueSpacing.SM.dp))
    Text(
        text = "Only you see this name. It stays on your phone.",
        style = ContinueTextStyles.label,
        color = ContinueColors.TextTertiary,
    )
    Spacer(Modifier.height(ContinueSpacing.MD.dp))
    ArcadeButton(
        text = "ADD TO FRIENDS",
        onClick = { onAdd(name) },
        enabled = FriendRepository.cleanName(name) != null,
        accent = ContinueColors.AccentNeon,
        modifier = Modifier.fillMaxWidth(),
    )

    if (state.existingFriends.isNotEmpty()) {
        Spacer(Modifier.height(ContinueSpacing.SM.dp))
        TextButton(onClick = { showExisting = !showExisting }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showExisting) "Hide" else "New phone? Update a friend you already follow")
        }
        AnimatedVisibility(visible = showExisting) {
            Column(verticalArrangement = Arrangement.spacedBy(ContinueSpacing.XS.dp)) {
                Text(
                    text = "Their pile will be replaced with this one and future links from this " +
                        "phone will update it.",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextTertiary,
                )
                state.existingFriends.forEach { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ContinueColors.SurfaceRaised)
                            .clickable { onReplace(friend) }
                            .padding(ContinueSpacing.SM.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FriendAvatar(friend.name, size = 32.dp)
                        Spacer(Modifier.width(ContinueSpacing.SM.dp))
                        Text(
                            text = friend.name,
                            style = ContinueTextStyles.body,
                            color = ContinueColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(text = "UPDATE", style = ContinueTextStyles.label, color = ContinueColors.AccentCoin)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Message(
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>? = null,
    accent: Color = ContinueColors.AccentCoin,
) {
    Text(text = title, style = ContinueTextStyles.titleL, color = accent)
    Spacer(Modifier.height(ContinueSpacing.SM.dp))
    Text(text = body, style = ContinueTextStyles.body, color = ContinueColors.TextSecondary)
    Spacer(Modifier.height(ContinueSpacing.LG.dp))
    ArcadeButton(text = primary.first, onClick = primary.second, accent = accent, modifier = Modifier.fillMaxWidth())
    if (secondary != null) {
        TextButton(onClick = secondary.second, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(secondary.first)
        }
    }
}

internal fun diffLine(diff: PileDiff): String {
    if (diff.isEmpty) return "Nothing changed since the last time they shared."
    val parts = buildList {
        if (diff.added > 0) add("${diff.added} new")
        if (diff.newlyCleared > 0) add("${diff.newlyCleared} newly cleared")
        if (diff.removed > 0) add("${diff.removed} gone")
    }
    return parts.joinToString(" · ") + " since the last time they shared."
}
