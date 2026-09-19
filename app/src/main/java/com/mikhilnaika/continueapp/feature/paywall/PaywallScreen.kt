package com.mikhilnaika.continueapp.feature.paywall

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikhilnaika.continueapp.core.billing.ProTier
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueShapes
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.ui.ArcadeButton
import com.mikhilnaika.continueapp.core.ui.LocalHaptics
import com.mikhilnaika.continueapp.core.util.findActivity
import com.mikhilnaika.continueapp.core.ads.FreePlay
import com.mikhilnaika.continueapp.core.audio.LocalArcadeAudio
import com.mikhilnaika.continueapp.core.audio.MusicCue
import com.mikhilnaika.continueapp.core.audio.MusicTrack
import com.mikhilnaika.continueapp.core.audio.Sfx

/**
 * GO PRO — hand-built rather than RevenueCatUI's dashboard-rendered paywall.
 *
 * The trade-off is deliberate. A dashboard paywall can be re-themed without shipping an APK,
 * which is genuinely valuable; but its templates can't produce the arcade cabinet this app is,
 * and a paywall that looks like every other RevenueCat paywall is a bad answer in a design
 * category. Pricing still comes from the store at runtime (`ProTier.priceFormatted` is
 * `Price.formatted`), so a price change in Play Console still needs no code change — which was
 * the part of the dashboard's value that actually mattered.
 *
 * Everything on screen is a real claim the app already delivers. No "coming soon" tiles.
 */
@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val audio = LocalArcadeAudio.current

    // The shop counter. Quiet and unhurried on purpose: a paywall that sounds urgent is a
    // paywall that feels like it's pushing.
    MusicCue(MusicTrack.SHOP)
    LaunchedEffect(state.error) { if (state.error != null) audio.play(Sfx.ERROR) }

    // Leaving on success rather than showing a confirmation screen: the entitlement propagates
    // through RevenueCat's customer-info listener, so the screen the user came from is already
    // correct by the time they see it again. A "thanks!" interstitial would just be in the way.
    LaunchedEffect(state.purchased) {
        if (state.purchased) {
            haptics.celebratory()
            // SoundPool plays independently of this screen, so the power-up carries on over the
            // dismiss rather than being cut off by it.
            audio.play(Sfx.POWER_UP)
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ContinueColors.SurfaceVoid)
            .scanlines(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ContinueSpacing.XL.dp)
                .padding(bottom = ContinueSpacing.XL.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = ContinueColors.TextTertiary,
                    )
                }
            }

            Marquee()

            Spacer(modifier = Modifier.height(ContinueSpacing.XL.dp))
            PerkList()
            Spacer(modifier = Modifier.height(ContinueSpacing.XL.dp))

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(ContinueSpacing.XXL.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = ContinueColors.AccentCoin) }

                state.isUnavailable -> OutOfOrder()

                else -> {
                    state.tiers.forEach { tier ->
                        TierCard(
                            tier = tier,
                            selected = tier.id == state.selectedTierId,
                            onSelect = {
                                haptics.light()
                                audio.play(Sfx.BLIP)
                                viewModel.selectTier(tier.id)
                            },
                        )
                        Spacer(modifier = Modifier.height(ContinueSpacing.MD.dp))
                    }

                    Spacer(modifier = Modifier.height(ContinueSpacing.SM.dp))
                    ArcadeButton(
                        text = state.selectedTier.ctaLabel(state.isPurchasing),
                        onClick = { context.findActivity()?.let(viewModel::purchase) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(ContinueSpacing.SM.dp))
                    FinePrint(tier = state.selectedTier)

                    Spacer(modifier = Modifier.height(ContinueSpacing.LG.dp))
                    FreePlayOffer(
                        status = state.freePlayStatus,
                        onStart = { context.findActivity()?.let(viewModel::startFreePlay) },
                    )

                    Spacer(modifier = Modifier.height(ContinueSpacing.MD.dp))
                    Text(
                        text = if (state.isRestoring) "RESTORING…" else "RESTORE PURCHASE",
                        style = ContinueTextStyles.label,
                        color = ContinueColors.TextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !state.isRestoring) { viewModel.restore() }
                            .padding(vertical = ContinueSpacing.SM.dp),
                    )
                }
            }

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(ContinueSpacing.MD.dp))
                Text(
                    text = error,
                    style = ContinueTextStyles.label,
                    color = ContinueColors.AccentHot,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.dismissError() },
                )
            }

            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(ContinueSpacing.LG.dp))
        }
    }
}

/**
 * FREE PLAY on the paywall: the honest answer to "is it worth it?" is to let people find out.
 * Secondary to the purchase on purpose — outlined, not filled — but on the same screen, because
 * the person most likely to watch an ad for an hour of PRO is the one already looking at PRO.
 */
@Composable
private fun FreePlayOffer(status: String?, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ContinueColors.AccentHot.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(enabled = status == null, onClick = onStart)
            .padding(horizontal = ContinueSpacing.LG.dp, vertical = ContinueSpacing.MD.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = status ?: "NOT SURE YET? ▸ FREE PLAY",
            style = ContinueTextStyles.titleM,
            color = ContinueColors.AccentHot,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Watch one ad for ${FreePlay.MINUTES} minutes of everything above.",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** The cabinet marquee: the app's own name in lights, with the tagline under it. */
@Composable
private fun Marquee() {
    // A slow, shallow pulse — an arcade marquee is lit, not strobing. Kept well inside the
    // range that could bother anyone photosensitive.
    val transition = rememberInfiniteTransition(label = "marquee")
    val glow by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "marqueeGlow",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CONTINUE?",
            style = ContinueTextStyles.displayXl,
            color = ContinueColors.AccentHot.copy(alpha = glow),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "PRO",
            style = ContinueTextStyles.displayXl,
            color = ContinueColors.AccentCoin,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(ContinueSpacing.MD.dp))
        Text(
            text = "The games you started deserve an ending.",
            style = ContinueTextStyles.body,
            color = ContinueColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * What PRO actually is. Every line is a capability that exists in the shipped build — the
 * free-tier limit it removes is stated so the value is checkable rather than asserted.
 */
private val PERKS = listOf(
    Triple("∞", "UNLIMITED DRAWS", "Free gets one a day, then the countdown."),
    Triple("▦", "UNLIMITED STACKS", "Free stops at two shelves."),
    // Granted by RevenueCat (COIN product grants: Lifetime 600 once, Monthly 50 per renewal) and
    // read back in by BillingRepository.syncStoreCoins — so this line is literally true per tier.
    // If the grants in the RevenueCat dashboard change, this line must change with them.
    Triple("◈", "COIN DROPS", "Lifetime: 600 coins up front. Monthly: 50 more every month."),
    Triple("◎", "NEVER WATCH AN AD AGAIN", "Ads are always optional here. Now they're gone."),
)

@Composable
private fun PerkList() {
    Column(verticalArrangement = Arrangement.spacedBy(ContinueSpacing.MD.dp)) {
        PERKS.forEach { (glyph, title, detail) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(ContinueColors.SurfaceRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = glyph, style = ContinueTextStyles.monoL, color = ContinueColors.AccentNeon)
                }
                Column(modifier = Modifier.padding(start = ContinueSpacing.MD.dp)) {
                    Text(text = title, style = ContinueTextStyles.titleM, color = ContinueColors.TextPrimary)
                    Text(text = detail, style = ContinueTextStyles.label, color = ContinueColors.TextTertiary)
                }
            }
        }
    }
}

@Composable
private fun TierCard(tier: ProTier, selected: Boolean, onSelect: () -> Unit) {
    val accent = if (tier.isLifetime) ContinueColors.AccentCoin else ContinueColors.AccentCool
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp))
            .background(if (selected) ContinueColors.SurfaceRaised else ContinueColors.SurfaceCabinet)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else ContinueColors.OutlineDim,
                shape = RoundedCornerShape(ContinueShapes.RADIUS_CARD_DP.dp),
            )
            .clickable(onClick = onSelect)
            .padding(ContinueSpacing.LG.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = tier.label, style = ContinueTextStyles.titleL, color = accent)
            Spacer(modifier = Modifier.weight(1f))
            if (tier.isLifetime) Badge(text = "BEST VALUE", color = ContinueColors.AccentCoin)
            else if (tier.freeTrialDays != null) {
                Badge(text = "${tier.freeTrialDays}-DAY FREE TRIAL", color = ContinueColors.AccentNeon)
            }
        }
        Spacer(modifier = Modifier.height(ContinueSpacing.XS.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = tier.priceFormatted, style = ContinueTextStyles.displayL, color = ContinueColors.TextPrimary)
            tier.cadence?.let {
                Text(
                    text = " $it",
                    style = ContinueTextStyles.label,
                    color = ContinueColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
        Text(
            text = if (tier.isLifetime) "One payment. Yours for good." else "Cancel any time in Google Play.",
            style = ContinueTextStyles.label,
            color = ContinueColors.TextTertiary,
        )
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text = text,
        style = ContinueTextStyles.label,
        color = ContinueColors.SurfaceVoid,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * Subscription terms in plain words.
 *
 * Google Play policy requires renewal terms and the cancellation path to be visible *before*
 * purchase, not buried in a store listing — and a trial that converts silently is the fastest
 * way to earn a refund and a one-star review.
 */
@Composable
private fun FinePrint(tier: ProTier?) {
    val text = when {
        tier == null -> ""
        tier.isLifetime -> "A single charge. No subscription, nothing to cancel."
        tier.freeTrialDays != null ->
            "Free for ${tier.freeTrialDays} days, then ${tier.priceFormatted} ${tier.cadence}. " +
                "Renews until you cancel. Cancel any time in Google Play — keep PRO until the period ends."
        else ->
            "${tier.priceFormatted} ${tier.cadence}, renews until you cancel. " +
                "Cancel any time in Google Play."
    }
    Text(
        text = text,
        style = ContinueTextStyles.label,
        color = ContinueColors.TextTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OutOfOrder() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = ContinueSpacing.XL.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "OUT OF ORDER",
            style = ContinueTextStyles.displayL,
            color = ContinueColors.AccentHot,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(ContinueSpacing.MD.dp))
        Text(
            text = "The store isn't taking coins on this build yet.\n" +
                "Everything in CONTINUE? still works without PRO.",
            style = ContinueTextStyles.body,
            color = ContinueColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The CTA says what the tap actually does, in the cabinet's voice.
 *
 * The lifetime tier read "INSERT COIN" next to the monthly tier's "SUBSCRIBE", which a closed
 * tester read as the rewarded-ad button rather than a purchase — "INSERT COIN" is the app's
 * label for *earning* a coin by watching an ad (DRAW's gate, `DrawGateScreen`), so reusing it
 * for a one-off payment was actively misleading, not just inconsistent.
 */
private fun ProTier?.ctaLabel(purchasing: Boolean): String = when {
    purchasing -> "…"
    this == null -> "INSERT COIN"
    freeTrialDays != null -> "START $freeTrialDays FREE DAYS ▸"
    isLifetime -> "PURCHASE ▸"
    else -> "SUBSCRIBE ▸"
}

/**
 * CRT scanlines — docs/03-DESIGN-SYSTEM.md's house texture.
 *
 * Drawn as a tiled linear gradient rather than N composables: this covers a full scrolling
 * screen, and one shader beats several hundred `Box`es for the same look.
 */
private fun Modifier.scanlines(): Modifier = this.drawBehind {
    val brush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.022f), Color.Transparent),
        start = Offset.Zero,
        end = Offset(0f, 3f),
        tileMode = androidx.compose.ui.graphics.TileMode.Repeated,
    )
    drawRect(brush = brush, size = Size(size.width, size.height))
}
