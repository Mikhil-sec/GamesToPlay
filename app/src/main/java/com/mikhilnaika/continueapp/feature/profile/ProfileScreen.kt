package com.mikhilnaika.continueapp.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles

/**
 * YOU tab — docs/02-PRODUCT-SPEC.md §7. High scores, stats, trophies, and settings
 * (including the mandatory IGDB attribution) are later-week polish; this Phase-1 stub only
 * carries the attribution requirement, since that must be present wherever IGDB data shows.
 */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "YOU", style = ContinueTextStyles.displayL, color = ContinueColors.TextPrimary)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 24.dp))
        Text(
            text = "High scores, stats, and trophies land in later weeks (docs/06-BUILD-ROADMAP.md).",
            style = ContinueTextStyles.body,
            color = ContinueColors.TextSecondary,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 32.dp))
        Text(
            text = "The data was freely provided by IGDB.com",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextTertiary,
        )
    }
}
