package com.mikhilnaika.continueapp.feature.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.EmptyState

/**
 * STATS — what your pile is actually made of.
 *
 * Asked for in closed testing off the back of the filter overhaul: *"based on the filters,
 * there could be a cool visual stats screen showing the diversity of games sitting in THE PILE
 * and CLEARED or/and the other categories"*. Every bar on this screen is a category you can
 * tap on PILE, because both come from `GameTaxonomy` — the screen is a picture of the filters
 * rather than a separate report that happens to be about the same games.
 *
 * Drawn entirely with layout: bars are `Box`es with a width fraction, the spread is a `Row` of
 * weights. No chart library, no `Canvas` — which keeps it theme-correct, accessible to
 * TalkBack as ordinary text, and free of another dependency in a public repo.
 */
@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snapshot = state.snapshot

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ContinueSpacing.LG.dp,
            end = ContinueSpacing.LG.dp,
            top = ContinueSpacing.LG.dp,
            // Clears the raised DRAW button the same way every other list does.
            bottom = ContinueSpacing.XXL.dp,
        ),
    ) {
        item(key = "title") {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "STATS",
                    style = ContinueTextStyles.displayL,
                    color = ContinueColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                // The bottom bar can get you out of here, but not back to where you came from —
                // STATS is opened from PILE's header, so it owes you a way back to it.
                TextButton(onClick = onBack) {
                    Text("BACK", style = ContinueTextStyles.label, color = ContinueColors.TextSecondary)
                }
            }
        }

        item(key = "spread") {
            SectionHeader("THE SPREAD")
            SpreadBar(slices = snapshot.stateSlices)
        }

        item(key = "scope") {
            SectionHeader("LOOKING AT")
            ScopeChips(selected = state.scope, onSelect = viewModel::selectScope)
        }

        if (snapshot.isEmpty) {
            item(key = "empty") {
                EmptyState(
                    headline = "NOTHING IN ${state.scope.label} YET",
                    supporting = "Add a few games and this screen fills itself in.",
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }
            return@LazyColumn
        }

        item(key = "headline") {
            Spacer(modifier = Modifier.height(ContinueSpacing.LG.dp))
            HeadlineTiles(snapshot)
        }

        item(key = "diversity") {
            SectionHeader("DIVERSITY")
            DiversityMeter(percent = snapshot.diversity, headlineFacet = snapshot.headlineFacet)
        }

        barSection(key = "facets", title = "GENRE & MOOD", bars = snapshot.facets, accent = ContinueColors.AccentNeon)
        barSection(key = "length", title = "LENGTH", bars = snapshot.lengths, accent = ContinueColors.AccentCoin)
        barSection(key = "platform", title = "PLATFORM", bars = snapshot.platforms, accent = ContinueColors.AccentCool)
        barSection(key = "decade", title = "RELEASED", bars = snapshot.decades, accent = ContinueColors.AccentHot)

        item(key = "igdb") {
            Spacer(modifier = Modifier.height(ContinueSpacing.XL.dp))
            Text(
                text = "The data was freely provided by IGDB.com",
                style = ContinueTextStyles.label,
                color = ContinueColors.TextTertiary,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.barSection(
    key: String,
    title: String,
    bars: List<StatBar>,
    accent: Color,
) {
    if (bars.isEmpty()) return
    item(key = key) {
        SectionHeader(title)
        Column {
            bars.forEach { bar -> BarRow(bar = bar, accent = accent) }
        }
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

/**
 * The whole pile as one bar, whatever scope is selected.
 *
 * Always shows every state, even the one being looked at, because the point of the top of this
 * screen is proportion: 40 in THE PILE against 3 CLEARED is the number a backlog app exists to
 * make you feel, and it disappears the moment you only ever see one category at a time.
 */
@Composable
private fun SpreadBar(slices: List<StateSlice>) {
    val total = slices.sumOf { it.count }
    if (total == 0) {
        Text(
            text = "NOTHING IN THE PILE YET",
            style = ContinueTextStyles.body,
            color = ContinueColors.TextTertiary,
        )
        return
    }
    val colors = listOf(
        ContinueColors.AccentCool,
        ContinueColors.AccentNeon,
        ContinueColors.AccentCoin,
        ContinueColors.TextTertiary,
        ContinueColors.AccentHot,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ContinueColors.SurfaceRaised),
    ) {
        slices.forEachIndexed { index, slice ->
            if (slice.count == 0) return@forEachIndexed
            Box(
                modifier = Modifier
                    .weight(slice.count.toFloat())
                    .fillMaxHeight()
                    .background(colors[index % colors.size]),
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = ContinueSpacing.SM.dp),
        horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.MD.dp),
    ) {
        slices.forEachIndexed { index, slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors[index % colors.size]),
                )
                Text(
                    text = " ${slice.label} ${slice.count}",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ScopeChips(selected: StatsScope, onSelect: (StatsScope) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp),
    ) {
        StatsScope.options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.label) },
            )
        }
    }
}

@Composable
private fun HeadlineTiles(snapshot: StatsSnapshot) {
    Row(horizontalArrangement = Arrangement.spacedBy(ContinueSpacing.SM.dp)) {
        Tile("GAMES", "${snapshot.totalGames}", modifier = Modifier.weight(1f))
        Tile("HOURS", "${snapshot.totalHours}", modifier = Modifier.weight(1f))
        Tile(
            label = "SPAN",
            value = if (snapshot.oldestYear != null && snapshot.newestYear != null) {
                if (snapshot.oldestYear == snapshot.newestYear) "${snapshot.oldestYear}"
                else "${snapshot.oldestYear}-${snapshot.newestYear % 100}"
            } else {
                "—"
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ContinueColors.SurfaceRaised)
            .padding(ContinueSpacing.MD.dp),
    ) {
        Text(
            text = value,
            style = ContinueTextStyles.monoL,
            color = ContinueColors.AccentCoin,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = label, style = ContinueTextStyles.label, color = ContinueColors.TextSecondary)
    }
}

/**
 * The one number on this screen that isn't just a count.
 *
 * Normalised entropy over the facet mix (see [PileStats.diversityPercent]), which is the honest
 * way to answer "how varied is this?" — counting categories would call nineteen shooters and
 * one puzzle game diverse. The verdict line underneath exists because a bare percentage of
 * something nobody has a reference point for means nothing.
 */
@Composable
private fun DiversityMeter(percent: Int, headlineFacet: String?) {
    val animated by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(700),
        label = "diversity",
    )
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = "$percent", style = ContinueTextStyles.monoL, color = ContinueColors.AccentNeon)
            Text(
                text = "% VARIED",
                style = ContinueTextStyles.label,
                color = ContinueColors.TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(ContinueColors.SurfaceRaised),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(ContinueColors.AccentNeon),
            )
        }
        Text(
            text = verdict(percent, headlineFacet),
            style = ContinueTextStyles.body,
            color = ContinueColors.TextTertiary,
            modifier = Modifier.padding(top = ContinueSpacing.SM.dp),
        )
    }
}

private fun verdict(percent: Int, headlineFacet: String?): String {
    val focus = headlineFacet?.lowercase()
    return when {
        percent >= 75 -> "All over the map, in the best way."
        percent >= 50 -> "A decent spread, leaning ${focus ?: "one way"}."
        percent > 0 -> "You know what you like: mostly ${focus ?: "one thing"}."
        else -> "One flavour only — ${focus ?: "all the same"}."
    }
}

@Composable
private fun BarRow(bar: StatBar, accent: Color) {
    val animated by animateFloatAsState(
        targetValue = bar.share.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "bar-${bar.label}",
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = bar.label,
            style = ContinueTextStyles.label,
            color = ContinueColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(104.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ContinueColors.SurfaceRaised),
        ) {
            Box(
                modifier = Modifier
                    // A bar with a real count never renders as nothing: a 1-of-40 bar would be
                    // sub-pixel, and an invisible bar reads as a missing category rather than a
                    // rare one.
                    .fillMaxWidth(animated.coerceAtLeast(0.04f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent),
            )
        }
        Text(
            text = "${bar.count}",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextPrimary,
            modifier = Modifier.width(36.dp).padding(start = ContinueSpacing.SM.dp),
        )
    }
}
