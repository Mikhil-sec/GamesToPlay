package com.mikhilnaika.continueapp.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.mikhilnaika.continueapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google's User Messaging Platform — the consent flow that makes it legal to serve ads to
 * users in the **EEA, the UK and Switzerland**.
 *
 * **Why this exists instead of a country exclusion.** The plan of record until 2026-09-12 was
 * to exclude those 32 countries from Play availability rather than build this. Two things
 * killed that plan: a Shipaton judge in any of them would see *"not available in your
 * country"* rather than the app, and "we removed a third of the developed world instead of
 * showing a dialog" is a strange position for a submission whose entire advertising thesis is
 * that CONTINUE? does ads more respectfully than everyone else.
 *
 * **The gate lives inside the capability, not at the call sites.** [RealAdRepository] asks
 * [canRequestAds] itself before it will load anything, so there is no way to request an ad
 * that skips the consent check — because there is no other way to get an ad. This codebase has
 * been bitten three times by rules enforced "at the call site" (the HAPTICS toggle wired to
 * nothing for weeks, the farmable clear reward, the share target's stub writes); this is the
 * same shape and gets the same treatment.
 *
 * **What [canRequestAds] actually means — this is easy to get wrong.** It reports whether the
 * consent flow has *completed*, **not** what the user decided: it returns `true` after a
 * "do not consent" just as it does after a "consent". So this class gates on *timing*, not on
 * approval, and a user who declines still gets ads — Google serves **non-personalised** ones,
 * driven by the TCF consent signal the UMP form writes, which the Mobile Ads SDK reads on its
 * own. Do not "fix" this by treating a false result as a refusal; false only ever means the
 * check has not finished, which is exactly the moment it is unsafe to request anything.
 *
 * **Nothing outside advertising depends on it.** Declining costs a European user nothing: the
 * pile, DRAW, RANK, sharing and offline are untouched, rewarded ads are still available (just
 * non-personalised), and coins are still earned by clearing games or bought outright.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val _privacyOptionsRequired = MutableStateFlow(false)

    /**
     * Whether Google requires us to show a persistent "Privacy options" entry point.
     *
     * This is **not** cosmetic: when a user is in a region where consent was collected, Google's
     * policy requires a way to reopen the form and change their mind, and an app that collects
     * consent without offering it is non-compliant. Surfaced in the YOU tab, and only there,
     * only when this is true — showing it to a user in Mauritius would be a settings row that
     * opens an empty form.
     */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired

    /**
     * True once it is lawful to request ads.
     *
     * Deliberately fails **closed**: the UMP SDK returns false until [requestConsentInfo] has
     * completed, so an ad requested before the check finishes is simply not loaded rather than
     * being loaded on the assumption that consent probably isn't needed here. Outside the
     * EEA/UK/CH this flips to true on the first launch callback with no form shown at all.
     */
    fun canRequestAds(): Boolean = consentInformation.canRequestAds()

    /**
     * Called on every app launch, as Google requires — consent status can change server-side
     * (a new vendor, a policy update) and is not a one-time question.
     *
     * Failures are swallowed on purpose. If the consent info can't be fetched, [canRequestAds]
     * stays false, which means no ads — the safe direction. There is nothing useful to tell the
     * user about a background compliance check, and an error dialog on cold start over a
     * network hiccup would be a far worse bug than a missing ad.
     */
    fun requestConsentInfo(activity: Activity, onComplete: () -> Unit = {}) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            consentParameters(),
            {
                // The form only appears when a form is actually required; elsewhere this
                // callback returns immediately having shown nothing.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    _privacyOptionsRequired.value = isPrivacyOptionsRequired()
                    onComplete()
                }
            },
            {
                _privacyOptionsRequired.value = isPrivacyOptionsRequired()
                onComplete()
            },
        )
    }

    /** Reopens the form from the YOU tab so a user can change an answer they already gave. */
    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            _privacyOptionsRequired.value = isPrivacyOptionsRequired()
        }
    }

    private fun isPrivacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * In debug, force the SDK to behave as though the device is in the EEA.
     *
     * Without this the flow is **untestable from Mauritius** — `requestConsentInfoUpdate`
     * would report that no form is needed and return, so every local run would exercise the
     * one path that does nothing, and the branch that actually matters would ship having never
     * executed. `addTestDeviceHashedId` takes the hashed id AdMob prints to logcat on the first
     * run; it is only consulted in debug builds.
     */
    private fun consentParameters(): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (BuildConfig.DEBUG) {
            builder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(context)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .build()
            )
        }
        return builder.build()
    }

    /** Debug-only: clears the stored decision so the form can be seen again on the next launch. */
    fun resetForTesting() {
        if (BuildConfig.DEBUG) consentInformation.reset()
    }
}
