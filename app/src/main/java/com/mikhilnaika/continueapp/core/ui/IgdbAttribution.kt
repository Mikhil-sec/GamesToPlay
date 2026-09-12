package com.mikhilnaika.continueapp.core.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles

private const val IGDB_URL = "https://www.igdb.com"

/**
 * IGDB's required user-facing credit — *"The data was freely provided by IGDB.com"*.
 *
 * **Contractual, not decorative.** CLAUDE.md constraint #3 and IGDB's partnership terms both
 * require this wherever their data is shown, and "wherever" means every screen, not the one
 * screen that is most obviously about games. This component exists so that placing it is a
 * one-line decision rather than a copy-paste of a string that can drift — there were three
 * separate hand-rolled versions of it in the app, with different styling and none of them
 * linked anywhere.
 *
 * **Designed to recede.** A credit that shouts competes with the app; a credit that hides
 * isn't a credit. So: a short centred hairline that fades out at both ends, generous space
 * above it, and the line itself in the dimmest text token — with only the words `IGDB.com`
 * lifted to secondary, which is enough to read as tappable without introducing an accent
 * colour into a place the eye shouldn't land.
 */
@Composable
fun IgdbAttribution(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val label = buildAnnotatedString {
        append("The data was freely provided by ")
        withStyle(SpanStyle(color = ContinueColors.TextSecondary)) { append("IGDB.com") }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = "Open IGDB.com",
            ) {
                // A device with no browser at all is vanishingly rare but entirely possible
                // (a kiosk, a stripped ROM), and an uncaught ActivityNotFoundException here
                // would crash the app from its own footer.
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, IGDB_URL.toUri())
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure { if (it !is ActivityNotFoundException) throw it }
            }
            .padding(vertical = ContinueSpacing.LG.dp, horizontal = ContinueSpacing.MD.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Hairline()
        Spacer(modifier = Modifier.height(ContinueSpacing.MD.dp))
        androidx.compose.material3.Text(
            text = label,
            style = ContinueTextStyles.label,
            color = ContinueColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

/** A short centred rule. Deliberately not full-width — a full rule reads as a section break. */
@Composable
private fun Hairline() {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(1.dp)
            .background(ContinueColors.OutlineDim),
    )
}
