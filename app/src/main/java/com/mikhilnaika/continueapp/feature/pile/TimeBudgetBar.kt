package com.mikhilnaika.continueapp.feature.pile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.design.monoStyle

/**
 * Compact form pinned under the top bar on PILE — docs/02-PRODUCT-SPEC.md §1:
 * "412 HRS · 87 GAMES · FINISHED BY 2029 ▸". The full-screen visualization it expands into
 * is a later polish item; this compact bar is the Week-1/2 scope.
 */
@Composable
fun TimeBudgetBar(
    totalHours: Int,
    totalGames: Int,
    finishCopy: String,
    modifier: Modifier = Modifier,
    onExpand: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ContinueColors.SurfaceCabinet)
            .clickable(enabled = onExpand != null) { onExpand?.invoke() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "$totalHours", style = monoStyle(size = 15.sp), color = ContinueColors.AccentCoin)
        Text(text = " HRS · ", style = ContinueTextStyles.label, color = ContinueColors.TextSecondary)
        Text(text = "$totalGames", style = monoStyle(size = 15.sp), color = ContinueColors.AccentCoin)
        Text(text = " GAMES · ", style = ContinueTextStyles.label, color = ContinueColors.TextSecondary)
        Text(
            text = finishCopy,
            style = ContinueTextStyles.label,
            color = ContinueColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        if (onExpand != null) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Expand time budget",
                tint = ContinueColors.TextSecondary,
            )
        }
    }
}
