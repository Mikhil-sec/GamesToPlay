package com.mikhilnaika.continueapp.feature.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.util.IgdbImage

private val AVATAR_ACCENTS = listOf(
    ContinueColors.AccentCoin,
    ContinueColors.AccentNeon,
    ContinueColors.AccentHot,
    ContinueColors.AccentCool,
)

/**
 * A friend's initial in an accent ring. The colour comes from the name, so the same friend is
 * the same colour everywhere and two friends side by side usually aren't.
 */
@Composable
fun FriendAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val accent = accentFor(name)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f))
            .border(2.dp, accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            style = ContinueTextStyles.titleL,
            color = accent,
        )
    }
}

fun accentFor(name: String): Color =
    AVATAR_ACCENTS[Math.floorMod(name.lowercase().hashCode(), AVATAR_ACCENTS.size)]

/** A fanned row of box art — the at-a-glance "this is what they're into". */
@Composable
fun CoverStrip(covers: List<String>, modifier: Modifier = Modifier, coverWidth: Dp = 36.dp) {
    if (covers.isEmpty()) return
    val overlap = coverWidth * 0.35f
    Box(modifier = modifier.width(coverWidth + (coverWidth - overlap) * (covers.size - 1))) {
        covers.forEachIndexed { index, url ->
            AsyncImage(
                model = IgdbImage.at(url, IgdbImage.GRID),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = (coverWidth - overlap) * index)
                    .width(coverWidth)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, ContinueColors.SurfaceVoid, RoundedCornerShape(6.dp))
                    .background(ContinueColors.SurfaceRaised),
            )
        }
    }
}

/** A row of big-number stats, arcade scoreboard style. */
@Composable
fun PileStatRow(stats: List<Pair<Int, String>>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
        stats.forEach { (value, label) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ContinueColors.SurfaceRaised)
                    .padding(vertical = ContinueSpacing.SM.dp, horizontal = ContinueSpacing.XS.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = value.toString(), style = ContinueTextStyles.monoL, color = ContinueColors.TextPrimary)
                Text(
                    text = label,
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextTertiary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Placeholder art for a game this device hasn't been able to name yet. */
@Composable
fun UnknownCover(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(ContinueColors.SurfaceRaised), contentAlignment = Alignment.Center) {
        Text(text = "?", style = ContinueTextStyles.displayL, color = ContinueColors.TextTertiary)
    }
}
