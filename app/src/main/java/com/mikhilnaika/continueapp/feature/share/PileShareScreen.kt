package com.mikhilnaika.continueapp.feature.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.share.ShareCardRenderer
import com.mikhilnaika.continueapp.core.ui.ArcadeButton

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ContinueColors.SurfaceVoid)
            .padding(ContinueSpacing.LG.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val card = state.card
        if (card != null) {
            Image(
                bitmap = card.asImageBitmap(),
                contentDescription =
                    "Share card: ${state.totalHours} hours across ${state.totalGames} games in your pile",
                modifier = Modifier
                    .fillMaxWidth()
                    // 4:5, matching the bitmap — `Crop` on a mismatched box was quietly
                    // cutting the card's own edges off in the preview.
                    .aspectRatio(0.8f)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Fit,
            )
        } else {
            // Holds the card's exact footprint so the button doesn't jump up the screen and
            // back down as the art finishes decoding.
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(0.8f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = ContinueColors.AccentCoin)
            }
        }

        Spacer(modifier = Modifier.height(ContinueSpacing.XL.dp))
        ArcadeButton(
            text = "SHARE",
            onClick = viewModel::share,
            enabled = card != null,
        )
        Spacer(modifier = Modifier.height(ContinueSpacing.SM.dp))
        Text(
            text = "CLOSE",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextTertiary,
            modifier = Modifier.padding(8.dp).clickable(onClick = onDismiss),
        )
    }
}
