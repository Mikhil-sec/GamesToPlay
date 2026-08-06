@file:OptIn(ExperimentalTextApi::class)

package com.mikhilnaika.continueapp.core.design

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mikhilnaika.continueapp.R

/**
 * Three faces, three roles — docs/03-DESIGN-SYSTEM.md §2.
 * Chakra Petch: display/arcade moments only. Inter: everything functional.
 * JetBrains Mono: counters, hours, ranks, coin balance (tabular figures).
 *
 * Hard rule: no pixel font below 16sp, never in a paragraph. This app uses
 * Chakra Petch (not a true pixel font) rather than Silkscreen, which keeps
 * every display size legible without violating that rule.
 */
object ContinueFonts {
    val ChakraPetch = FontFamily(
        Font(R.font.chakrapetch_regular, FontWeight.Normal),
        Font(R.font.chakrapetch_semibold, FontWeight.SemiBold),
        Font(R.font.chakrapetch_bold, FontWeight.Bold),
    )

    // Variable fonts: a single file, weight selected via FontVariation on the wght axis.
    val Inter = FontFamily(
        Font(R.font.inter_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.inter_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        Font(R.font.inter_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    )

    val JetBrainsMono = FontFamily(
        Font(R.font.jetbrainsmono_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.jetbrainsmono_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    )
}

/** Scale from docs/03-DESIGN-SYSTEM.md §2. */
object ContinueTextStyles {
    val displayXl = TextStyle(
        fontFamily = ContinueFonts.ChakraPetch,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.02.em,
    )
    val displayL = TextStyle(
        fontFamily = ContinueFonts.ChakraPetch,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    )
    val titleL = TextStyle(
        fontFamily = ContinueFonts.Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    )
    val titleM = TextStyle(
        fontFamily = ContinueFonts.Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    )
    val body = TextStyle(
        fontFamily = ContinueFonts.Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )
    val label = TextStyle(
        fontFamily = ContinueFonts.Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.em,
    )
    val monoL = TextStyle(
        fontFamily = ContinueFonts.JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
    )
}

@Composable
fun continueTypography(): Typography = Typography(
    displayLarge = ContinueTextStyles.displayXl,
    displayMedium = ContinueTextStyles.displayL,
    titleLarge = ContinueTextStyles.titleL,
    titleMedium = ContinueTextStyles.titleM,
    bodyLarge = ContinueTextStyles.body,
    labelLarge = ContinueTextStyles.label,
)

/** Numerals with tabular figures so they don't jitter when animating (coin balance, hours, ranks). */
fun monoStyle(size: TextUnit = 17.sp, weight: FontWeight = FontWeight.Bold): TextStyle = TextStyle(
    fontFamily = ContinueFonts.JetBrainsMono,
    fontWeight = weight,
    fontSize = size,
)
