package com.mikhilnaika.continueapp.feature.draw

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles

/**
 * Placeholder for the signature DRAW machine — dials, lever physics, weighted selection,
 * and the card deal all belong to Week 3 of docs/06-BUILD-ROADMAP.md, not this Phase-1
 * scaffold. This screen exists so the nav bar's DRAW button has somewhere to go today.
 */
@Composable
fun DrawPlaceholderScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "THE MACHINE IS BEING BUILT",
            style = ContinueTextStyles.titleL,
            color = ContinueColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
        Text(
            text = "DRAW's dials, lever, and card deal land in the Week 3 build.",
            style = ContinueTextStyles.body,
            color = ContinueColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
