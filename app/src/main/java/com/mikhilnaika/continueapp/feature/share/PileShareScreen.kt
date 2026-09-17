package com.mikhilnaika.continueapp.feature.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.mikhilnaika.continueapp.core.design.ContinueShapes
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.share.ShareCardRenderer
import com.mikhilnaika.continueapp.core.ui.ArcadeButton

/**
 * "SHARE YOUR PILE" — docs/02-PRODUCT-SPEC.md §6: "self-deprecating, universally relatable — the
 * viral one." The bitmap shown here is exactly what gets shared (rendered natively — see
 * [ShareCardRenderer]), so the preview is never out of sync with the PNG.
 *
 * Since versionCode 11 the message also carries a follow link, so the screen says plainly what
 * that link contains. A user should never discover after the fact that "a picture of my pile"
 * also listed every game in it.
 */
@Composable
fun PileShareScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PileShareViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ContinueColors.SurfaceVoid)
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(ContinueSpacing.LG.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "SHARE YOUR PILE",
                style = ContinueTextStyles.titleL,
                color = ContinueColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(ContinueSpacing.MD.dp))

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

            Spacer(modifier = Modifier.height(ContinueSpacing.LG.dp))
            FollowLinkNote()

            Spacer(modifier = Modifier.height(ContinueSpacing.XL.dp))
            ArcadeButton(
                text = if (state.isSharing) "PREPARING…" else "SHARE",
                onClick = viewModel::share,
                enabled = card != null && !state.isSharing,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(ContinueSpacing.SM.dp))
            Text(
                text = "CLOSE",
                style = ContinueTextStyles.label,
                color = ContinueColors.TextTertiary,
                modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp),
            )
        }
    }
}

@Composable
private fun FollowLinkNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp))
            .background(ContinueColors.SurfaceCabinet)
            .border(1.dp, ContinueColors.OutlineDim, RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp))
            .padding(ContinueSpacing.MD.dp),
    ) {
        Text(
            text = "FRIENDS CAN FOLLOW IT",
            style = ContinueTextStyles.label,
            color = ContinueColors.AccentNeon,
        )
        Spacer(modifier = Modifier.height(ContinueSpacing.XS.dp))
        Text(
            text = "The link lists every game in your pile and where it's at — no name, no account. " +
                "Friends with CONTINUE? tap it to see your pile in FRIENDS. Share again any time " +
                "and their copy updates.",
            style = ContinueTextStyles.body,
            color = ContinueColors.TextSecondary,
        )
    }
}
