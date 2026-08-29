package com.mikhilnaika.continueapp.feature.clipboard

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueShapes
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.util.IgdbImage

/**
 * The clipboard nudge — docs/02-PRODUCT-SPEC.md §2d.
 *
 * Reads the clipboard on every foreground, but **only if the user has switched the setting on**,
 * which it is not by default. That opt-in isn't only a preference: from Android 12 the system
 * shows a "CONTINUE? pasted from your clipboard" toast every time an app reads clipboard content
 * it didn't write. Reading unprompted would put that toast on every single app open. Behind an
 * off-by-default switch, the only people who see it have asked for the feature.
 *
 * Read on `ON_RESUME` via a plain lifecycle observer rather than `LifecycleEventEffect`, which is
 * newer API surface for no gain here, and inside `runCatching` because OEM clipboard managers do
 * throw (`SecurityException`, and `DeadObjectException` on a few Samsung builds — this app's
 * primary test device is a Galaxy Tab).
 */
@Composable
fun ClipboardNudge(
    modifier: Modifier = Modifier,
    viewModel: ClipboardNudgeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val suggestion by viewModel.suggestion.collectAsState()
    val enabled by viewModel.isEnabled.collectAsState()
    val currentViewModel by rememberUpdatedState(viewModel)

    // Keyed on `enabled`, so with the setting off **no observer is registered and the clipboard
    // is never read** — not read-then-discarded. On Android 12+ the system toast fires on the
    // read itself, so gating anywhere later would still show every user "CONTINUE? pasted from
    // your clipboard" on every app open, opted in or not.
    //
    // Registering an observer while the owner is already RESUMED dispatches ON_RESUME
    // immediately, which is a feature here: switching the setting on in YOU checks the clipboard
    // there and then, and that read is the direct result of the user's own tap.
    DisposableEffect(lifecycleOwner, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentViewModel.onClipboardText(readClipboardText(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AnimatedVisibility(
        visible = suggestion != null,
        modifier = modifier,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        suggestion?.let { offer ->
            NudgeCard(
                name = offer.candidate.name,
                coverUrl = offer.candidate.coverUrl,
                onAdd = viewModel::add,
                onDismiss = viewModel::dismiss,
            )
        }
    }
}

/**
 * Whatever plain text is on the clipboard, or null.
 *
 * From API 29 an app may only read the clipboard while it holds focus — which is exactly when
 * this runs, so the restriction costs nothing. Everything else here is defensive: the clip can be
 * empty, can hold a non-text item, and the manager itself can throw.
 */
private fun readClipboardText(context: Context): String? = runCatching {
    val clipboard = context.getSystemService<ClipboardManager>() ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    clip.getItemAt(0)?.coerceToText(context)?.toString()
}.getOrNull()

@Composable
private fun NudgeCard(
    name: String,
    coverUrl: String?,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = ContinueSpacing.LG.dp,
                end = ContinueSpacing.LG.dp,
                // Clears the raised DRAW button, the same as every other bottom-anchored surface.
                bottom = ContinueSpacing.XXL.dp,
            )
            .clip(RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp))
            .background(ContinueColors.SurfaceRaised)
            .padding(ContinueSpacing.SM.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(4.dp))
                .background(ContinueColors.SurfaceCabinet),
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = IgdbImage.at(coverUrl, IgdbImage.GRID),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ON YOUR CLIPBOARD",
                style = ContinueTextStyles.label,
                color = ContinueColors.TextTertiary,
            )
            Text(
                text = name,
                style = ContinueTextStyles.body,
                color = ContinueColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onAdd) {
            Text("ADD", style = ContinueTextStyles.label, color = ContinueColors.AccentCoin)
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = ContinueColors.TextSecondary,
            )
        }
    }
}
