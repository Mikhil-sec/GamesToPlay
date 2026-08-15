package com.mikhilnaika.continueapp.feature.sharetarget

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueShapes
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.design.ContinueTheme
import com.mikhilnaika.continueapp.core.network.dto.ResolveCandidateDto
import dagger.hilt.android.AndroidEntryPoint

/**
 * Transparent bottom-sheet Activity — the full app never launches for a share. Registered
 * for `text/plain` and `image` mime types in AndroidManifest.xml (docs/05-TECH-ARCHITECTURE.md).
 */
@AndroidEntryPoint
class ShareTargetActivity : ComponentActivity() {

    private val viewModel: ShareTargetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            ContinueTheme {
                ShareSheet(
                    viewModel = viewModel,
                    onDismiss = { finish() },
                )
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                when {
                    imageUri != null -> viewModel.resolveImage(imageUri)
                    !text.isNullOrBlank() -> viewModel.resolveText(text, subject)
                    else -> viewModel.resolveText(null, subject)
                }
            }
            else -> viewModel.resolveText(null, null)
        }
    }
}

@Composable
private fun ShareSheet(viewModel: ShareTargetViewModel, onDismiss: () -> Unit) {
    val resolution by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            // The scrim is the outside-tap dismiss. `windowIsFloating=false` means the platform
            // won't hand us `windowCloseOnTouchOutside`, so without this there was genuinely no
            // way out of the sheet if you changed your mind — see the X in the header too.
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = ContinueShapes.RADIUS_SHEET_TOP_DP.dp, topEnd = ContinueShapes.RADIUS_SHEET_TOP_DP.dp))
                .background(ContinueColors.SurfaceCabinet)
                // Swallow taps on the sheet itself so they don't fall through to the scrim and
                // dismiss the thing the user is trying to use.
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ADD TO CONTINUE?",
                    style = ContinueTextStyles.titleL,
                    color = ContinueColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel — don't add this game",
                        tint = ContinueColors.TextSecondary,
                    )
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))

            when (val current = resolution) {
                is ShareResolutionState.Loading -> LoadingRow()
                is ShareResolutionState.Confident -> ConfidentRow(
                    candidate = current.candidate,
                    onAdd = { viewModel.addCandidate(current.candidate) },
                    onSearchInstead = viewModel::fallBackToManualEntry,
                )
                is ShareResolutionState.Ambiguous -> AmbiguousList(
                    candidates = current.candidates,
                    onPick = viewModel::addCandidate,
                    onSearchInstead = viewModel::fallBackToManualEntry,
                )
                is ShareResolutionState.ManualEntry -> ManualEntryField(
                    prefill = current.prefillText.orEmpty(),
                    onSearch = viewModel::searchManually,
                )
                is ShareResolutionState.Added -> AddedConfirmation(gameName = current.gameName, onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ContinueColors.AccentCoin)
    }
}

@Composable
private fun ConfidentRow(candidate: ResolveCandidateDto, onAdd: () -> Unit, onSearchInstead: () -> Unit) {
    Column {
        Text(text = candidate.name, style = ContinueTextStyles.titleM, color = ContinueColors.TextPrimary)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("ADD TO PILE") }
        TextButton(onClick = onSearchInstead) { Text("Not right? Search instead") }
    }
}

@Composable
private fun AmbiguousList(
    candidates: List<ResolveCandidateDto>,
    onPick: (ResolveCandidateDto) -> Unit,
    onSearchInstead: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Which one?", style = ContinueTextStyles.body, color = ContinueColors.TextSecondary)
        candidates.forEach { candidate ->
            Button(onClick = { onPick(candidate) }, modifier = Modifier.fillMaxWidth()) {
                Text(candidate.name)
            }
        }
        TextButton(onClick = onSearchInstead) { Text("None of these — search instead") }
    }
}

@Composable
private fun ManualEntryField(prefill: String, onSearch: (String) -> Unit) {
    var query by remember { mutableStateOf(prefill) }
    Column {
        Text(
            text = "Couldn't match that automatically — type the name",
            style = ContinueTextStyles.body,
            color = ContinueColors.TextSecondary,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Game name") },
            // The prefill is a whole video caption. Without this, correcting a bad match means
            // holding backspace through 60-odd characters before you can type anything.
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear",
                            tint = ContinueColors.TextSecondary,
                        )
                    }
                }
            },
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
        Button(onClick = { onSearch(query) }, modifier = Modifier.fillMaxWidth(), enabled = query.isNotBlank()) {
            Text("SEARCH")
        }
    }
}

@Composable
private fun AddedConfirmation(gameName: String, onDismiss: () -> Unit) {
    // Auto-dismisses in ~1.2s per docs/02-PRODUCT-SPEC.md §2a; the button below is the
    // tap-to-skip escape hatch every ritual in this app is required to have.
    androidx.compose.runtime.LaunchedEffect(gameName) {
        kotlinx.coroutines.delay(1200)
        onDismiss()
    }
    Text(text = "$gameName added to your pile", style = ContinueTextStyles.titleM, color = ContinueColors.AccentNeon)
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("DONE") }
}
