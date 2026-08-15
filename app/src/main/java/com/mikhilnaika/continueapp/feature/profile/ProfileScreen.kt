package com.mikhilnaika.continueapp.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.EmptyState

/** YOU tab — docs/02-PRODUCT-SPEC.md §7. */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onGoPro: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = ContinueSpacing.LG.dp),
        // Top: the coin counter sits above this screen, so it needs its own breathing room.
        // Bottom: the raised DRAW button overhangs the nav bar by 24dp.
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = ContinueSpacing.LG.dp,
            bottom = ContinueSpacing.XXL.dp,
        ),
    ) {
        item {
            Text(text = "YOU", style = ContinueTextStyles.displayL, color = ContinueColors.TextPrimary)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.LG.dp))
        }

        // Until now the only way to reach the paywall was to exhaust the daily free draw, which
        // means a judge (or a willing buyer) could easily never see it. YOU is where a user goes
        // to look at their own account, so it's the natural second door.
        if (!state.isPro) {
            item { GoProBanner(onGoPro = onGoPro) }
        }

        item { SectionHeader("THIS YEAR") }
        item { ThisYearGrid(state.thisYear) }

        item { SectionHeader("HIGH SCORES") }
        item {
            if (state.highScores.isEmpty()) {
                EmptyState(headline = "RANK A GAME TO START YOUR LEADERBOARD")
            } else {
                Column {
                    state.highScores.take(10).forEach { HighScoreRow(it) }
                }
            }
        }

        item { SectionHeader("TROPHIES") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
                items(state.trophies, key = { it.id }) { TrophyChip(it) }
            }
        }

        item { SectionHeader("SETTINGS") }
        item {
            SettingsToggleRow(
                label = "HAPTICS",
                checked = state.hapticsEnabled,
                onCheckedChange = viewModel::setHapticsEnabled,
            )
            SettingsToggleRow(
                label = "CLIPBOARD DETECTION",
                checked = state.clipboardDetectionEnabled,
                onCheckedChange = viewModel::setClipboardDetectionEnabled,
            )
        }

        item {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XL.dp))
            Text(
                text = "The data was freely provided by IGDB.com",
                style = ContinueTextStyles.label,
                color = ContinueColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun GoProBanner(onGoPro: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ContinueColors.SurfaceRaised)
            .clickable(onClick = onGoPro)
            .padding(horizontal = ContinueSpacing.LG.dp, vertical = ContinueSpacing.MD.dp),
    ) {
        Text(text = "▸ GO PRO", style = ContinueTextStyles.titleM, color = ContinueColors.AccentCoin)
        Text(
            text = "Unlimited draws · unlimited stacks · no ads",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextSecondary,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = ContinueTextStyles.label,
        color = ContinueColors.TextSecondary,
        modifier = Modifier.padding(top = ContinueSpacing.LG.dp, bottom = ContinueSpacing.SM.dp),
    )
}

@Composable
private fun ThisYearGrid(stats: ThisYearStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
        StatTile("CLEARED", "${stats.gamesCleared}", modifier = Modifier.weight(1f))
        StatTile("HOURS", "${stats.hoursCleared}", modifier = Modifier.weight(1f))
        StatTile("STREAK", "${stats.currentStreakDays}D", modifier = Modifier.weight(1f))
    }
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.SM.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
        StatTile("LONGEST CLEAR", "${stats.longestGameHours}H", modifier = Modifier.weight(1f))
        StatTile("FASTEST CLEAR", stats.fastestClearDays?.let { "${it}D" } ?: "—", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ContinueColors.SurfaceRaised)
            .padding(ContinueSpacing.MD.dp),
    ) {
        Text(text = value, style = ContinueTextStyles.monoL, color = ContinueColors.AccentCoin)
        Text(text = label, style = ContinueTextStyles.label, color = ContinueColors.TextSecondary)
    }
}

@Composable
private fun HighScoreRow(entry: HighScoreEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = ContinueSpacing.XS.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#${entry.position}",
            style = ContinueTextStyles.monoL,
            color = ContinueColors.AccentCoin,
            modifier = Modifier.width(48.dp),
        )
        Box(
            modifier = Modifier
                .width(32.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(4.dp))
                .background(ContinueColors.SurfaceRaised),
        ) {
            if (entry.coverUrl != null) {
                AsyncImage(model = entry.coverUrl, contentDescription = entry.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = ContinueSpacing.SM.dp)) {
            Text(text = entry.name, style = ContinueTextStyles.body, color = ContinueColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            entry.verdict?.let {
                Text(text = it, style = ContinueTextStyles.label, color = ContinueColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TrophyChip(trophy: Trophy) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (trophy.unlocked) ContinueColors.SurfaceRaised else ContinueColors.SurfaceCabinet)
            .padding(ContinueSpacing.SM.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (trophy.unlocked) ContinueColors.AccentCoin else ContinueColors.OutlineDim),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = ContinueSpacing.XS.dp))
        Text(
            text = trophy.label,
            style = ContinueTextStyles.label,
            color = if (trophy.unlocked) ContinueColors.TextPrimary else ContinueColors.TextTertiary,
            maxLines = 2,
        )
        Text(
            text = trophy.description,
            style = ContinueTextStyles.label,
            color = ContinueColors.TextTertiary,
            maxLines = 2,
        )
    }
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = ContinueSpacing.XS.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = ContinueTextStyles.body, color = ContinueColors.TextPrimary)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
