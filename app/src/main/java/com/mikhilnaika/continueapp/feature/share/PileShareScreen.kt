package com.mikhilnaika.continueapp.feature.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.share.ShareCardRenderer
import com.mikhilnaika.continueapp.core.ui.ArcadeButton
import kotlinx.coroutines.launch

/**
 * "THE PILE" share card — docs/02-PRODUCT-SPEC.md §6: "self-deprecating, universally
 * relatable — the viral one." The bitmap shown here is exactly what gets shared (rendered
 * natively — see [ShareCardRenderer]), so the preview is never out of sync with the PNG.
 */
@Composable
fun PileShareScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PileShareViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Compose lint's ProduceStateDoesNotAssignValue check wants exactly one unconditional
    // `value = …` statement in the lambda body — it flagged even a `value = null` followed by
    // an `if` containing a second assignment. The branching moved into [pileCardOrNull] so the
    // lambda itself has nothing conditional left to misread. The old shape was functionally
    // fine either way (isLoading flips true->false exactly once, which re-keys and reruns the
    // producer), but the check exists because the same shape is usually a real bug.
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, state.isLoading) {
        value = pileCardOrNull(state.isLoading, state.totalHours, state.totalGames)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ContinueColors.SurfaceVoid)
            .padding(ContinueSpacing.LG.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "THE PILE share card",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
        ArcadeButton(
            text = "SHARE",
            onClick = {
                val bmp = bitmap ?: return@ArcadeButton
                scope.launch { ShareCardRenderer.share(context, bmp, "the_pile") }
            },
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))
        Text(
            text = "CLOSE",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextTertiary,
            modifier = Modifier.padding(8.dp).clickable(onClick = onDismiss),
        )
    }
}

private suspend fun pileCardOrNull(isLoading: Boolean, totalHours: Int, totalGames: Int): android.graphics.Bitmap? {
    if (isLoading) return null
    return ShareCardRenderer.renderPileCard(totalHours, totalGames)
}
