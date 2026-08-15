package com.mikhilnaika.continueapp.core.ui

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
 * Never a blank screen — docs/03-DESIGN-SYSTEM.md §8. An arcade attract-mode line plus a
 * single clear action.
 */
@Composable
fun EmptyState(
    headline: String,
    modifier: Modifier = Modifier,
    /**
     * What the thing *is*, for empty states whose headline can't carry it alone. "NO STACKS
     * YET" is a status, not an explanation — the first tester to reach that screen asked what
     * stacks were for, which is a copy bug, not a feature gap.
     */
    supporting: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = headline,
            style = ContinueTextStyles.titleL,
            color = ContinueColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (supporting != null) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
            Text(
                text = supporting,
                style = ContinueTextStyles.body,
                color = ContinueColors.TextTertiary,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
            action()
        }
    }
}
