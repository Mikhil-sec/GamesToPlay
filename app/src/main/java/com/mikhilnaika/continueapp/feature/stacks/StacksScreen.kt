package com.mikhilnaika.continueapp.feature.stacks

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.data.entity.StackEntity
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.EmptyState

/** docs/02-PRODUCT-SPEC.md §1 "Stacks (collections)". */
@Composable
fun StacksScreen(
    modifier: Modifier = Modifier,
    viewModel: StacksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<StackEntity?>(null) }
    var editingStack by remember { mutableStateOf<StackEntity?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(ContinueSpacing.LG.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = "STACKS", style = ContinueTextStyles.displayL, color = ContinueColors.TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New stack", tint = ContinueColors.AccentCoin)
            }
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))

        if (state.stacks.isEmpty()) {
            EmptyState(
                headline = "NO STACKS YET",
                supporting = "A stack is your own shelf inside the pile — " +
                    "\"Steam Deck queue\", \"Halloween horror\", \"couch co-op with Sam\". " +
                    "A game can sit on as many shelves as you like, and the pile keeps it either way.",
                modifier = Modifier.weight(1f),
                action = {
                    com.mikhilnaika.continueapp.core.ui.ArcadeButton(text = "NEW STACK", onClick = { showCreateDialog = true })
                },
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
                state.stacks.forEach { stack ->
                    FilterChip(
                        selected = state.selectedStackId == stack.stackId,
                        onClick = { viewModel.selectStack(if (state.selectedStackId == stack.stackId) null else stack.stackId) },
                        label = { Text("${stack.emoji.orEmpty()} ${stack.name}".trim()) },
                    )
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.MD.dp))

            val selectedStack = state.stacks.find { it.stackId == state.selectedStackId }
            if (selectedStack != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${state.selectedStackMembers.size} GAMES",
                        style = ContinueTextStyles.label,
                        color = ContinueColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { editingStack = selectedStack }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename stack", tint = ContinueColors.TextSecondary)
                    }
                    IconButton(onClick = { pendingDelete = selectedStack }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete stack", tint = ContinueColors.AccentHot)
                    }
                }
                if (state.selectedStackMembers.isEmpty()) {
                    EmptyState(
                        headline = "ADD GAMES FROM THE PILE",
                        supporting = "Tap any game in THE PILE, then pick this stack under ADD TO STACK.",
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = ContinueSpacing.SM.dp),
                        horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
                        verticalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
                    ) {
                        items(state.selectedStackMembers, key = { it.gameId }) { member ->
                            Box {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ContinueColors.SurfaceCabinet),
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).background(ContinueColors.SurfaceRaised)) {
                                        if (member.coverUrl != null) {
                                            AsyncImage(model = member.coverUrl, contentDescription = member.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                    }
                                    Text(text = member.name, style = ContinueTextStyles.label, color = ContinueColors.TextPrimary, maxLines = 1, modifier = Modifier.padding(4.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.removeGameFromStack(selectedStack.stackId, member.gameId) },
                                    modifier = Modifier.align(Alignment.TopEnd),
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = ContinueColors.TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateStackDialog(
            title = "NEW STACK",
            onCreate = { name, emoji -> viewModel.createStack(name, emoji); showCreateDialog = false },
            onDismiss = { showCreateDialog = false },
        )
    }

    editingStack?.let { stack ->
        CreateStackDialog(
            title = "RENAME STACK",
            initialName = stack.name,
            initialEmoji = stack.emoji.orEmpty(),
            onCreate = { name, emoji -> viewModel.renameStack(stack.stackId, name, emoji); editingStack = null },
            onDismiss = { editingStack = null },
        )
    }

    pendingDelete?.let { stack ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("DELETE ${stack.name}?") },
            text = { Text("The games stay in your pile — only the stack goes away.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteStack(stack); pendingDelete = null }) { Text("DELETE") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("CANCEL") } },
            containerColor = ContinueColors.SurfaceRaised,
            textContentColor = ContinueColors.TextSecondary,
            titleContentColor = ContinueColors.TextPrimary,
        )
    }

    state.limitReachedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissLimitMessage,
            title = { Text("STACK LIMIT REACHED") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissLimitMessage) { Text("OK") } },
            containerColor = ContinueColors.SurfaceRaised,
            textContentColor = ContinueColors.TextSecondary,
            titleContentColor = ContinueColors.TextPrimary,
        )
    }
}

@Composable
private fun CreateStackDialog(
    title: String,
    onCreate: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "",
    initialEmoji: String = "",
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("e.g. Steam Deck queue") }, singleLine = true)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))
                OutlinedTextField(value = emoji, onValueChange = { emoji = it.take(2) }, placeholder = { Text("Emoji (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, emoji.ifBlank { null }) }, enabled = name.isNotBlank()) { Text("CREATE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
        containerColor = ContinueColors.SurfaceRaised,
        textContentColor = ContinueColors.TextSecondary,
        titleContentColor = ContinueColors.TextPrimary,
    )
}
