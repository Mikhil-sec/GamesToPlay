package com.mikhilnaika.continueapp.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueShapes
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.util.IgdbImage

/**
 * The grid/list unit everywhere in the app: box art, title, and an optional metadata line
 * ("SHORT · COZY · ON YOUR SWITCH" style annotations belong to the caller).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCard(
    title: String,
    coverUrl: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp))
            .background(ContinueColors.SurfaceCabinet)
            .let {
                when {
                    onLongClick != null -> it.combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
                    onClick != null -> it.clickable(onClick = onClick)
                    else -> it
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .background(ContinueColors.SurfaceRaised),
        ) {
            // Painted unconditionally, *underneath* the cover. A card whose art hasn't arrived
            // — no URL, still loading, or offline with nothing cached — then reads as the game
            // rather than as an empty grey slab, which is what a plane-mode pile looked like.
            Text(
                text = title.take(1).uppercase(),
                style = ContinueTextStyles.displayL,
                color = ContinueColors.TextTertiary,
                modifier = Modifier.align(Alignment.Center),
            )
            if (coverUrl != null) {
                AsyncImage(
                    // The Worker's baseline t_cover_big is 264px wide — under a card's real
                    // pixel width on any modern phone. See IgdbImage for the measurements.
                    model = IgdbImage.at(coverUrl, IgdbImage.GRID),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = title,
                style = ContinueTextStyles.titleM,
                color = ContinueColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
