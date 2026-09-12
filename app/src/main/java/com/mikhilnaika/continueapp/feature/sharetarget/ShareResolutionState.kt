package com.mikhilnaika.continueapp.feature.sharetarget

import com.mikhilnaika.continueapp.core.network.dto.ResolveCandidateDto

/**
 * The graceful-degradation ladder from docs/02-PRODUCT-SPEC.md §2a — the sheet must never
 * dead-end. Rungs, in order of how good the outcome is:
 *   1. [Confident] — one tap to add.
 *   2. [Ambiguous] — top 3 candidates, pick one.
 *   3. [ManualEntry] — unresolvable link or no match; pre-focused search field, text retained.
 * There's no explicit "offline" state here: [WorkerGameDataSource] already collapses a
 * network failure into `needsManualEntry = true`, so it naturally lands on rung 3.
 */
sealed interface ShareResolutionState {
    data object Loading : ShareResolutionState
    data class Confident(val candidate: ResolveCandidateDto) : ShareResolutionState
    data class Ambiguous(val candidates: List<ResolveCandidateDto>) : ShareResolutionState
    /**
     * @param note why auto-matching didn't happen, when there's something specific and useful
     *   to say. Null for the ordinary "we looked and found nothing" case, where the field's own
     *   standing copy already says it.
     */
    data class ManualEntry(val prefillText: String?, val note: String? = null) : ShareResolutionState
    data class Added(val gameName: String) : ShareResolutionState

    /**
     * A friend sent a game that's already on the pile.
     *
     * Its own rung rather than a silent success, because the friend loop makes this common —
     * people recommend each other the same well-known games — and [ShareTargetViewModel
     * .addCandidate] would otherwise skip the insert and still report "added to your pile",
     * which is a lie the user can check. Saying so also answers the question they actually
     * have, which is whether they already had it.
     */
    data class AlreadyInPile(val gameName: String) : ShareResolutionState
}

private const val CONFIDENCE_THRESHOLD = 0.85f

/**
 * TikTok is the one source where a *near* miss is worse than no guess at all.
 *
 * Its oEmbed hands back the video's caption, which is hashtag soup written for the algorithm
 * rather than a title — so a caption with no game in it still scores just high enough to fill
 * the chooser with three unrelated games ("Check this out" ranked *Check-In*, *Check Inn* and
 * *Wai-wai Check!*, all at 0.425). A tester reading that list has to notice it's wrong, back
 * out, and type the name anyway. When TikTok gives us a confident hit we still take it; short
 * of that we skip the middle rung and go straight to the field, and say why.
 */
internal const val TIKTOK_NOTE =
    "TikTok doesn't hand over a video's title, so CONTINUE? can't match this one for you — " +
        "type the game's name."

fun classify(
    resolvedTitle: String?,
    candidates: List<ResolveCandidateDto>,
    needsManualEntry: Boolean,
    rawText: String?,
    isTikTok: Boolean = false,
): ShareResolutionState =
    when {
        needsManualEntry || candidates.isEmpty() ->
            if (isTikTok) ShareResolutionState.ManualEntry(null, TIKTOK_NOTE)
            else ShareResolutionState.ManualEntry(rawText)
        candidates[0].confidence >= CONFIDENCE_THRESHOLD -> ShareResolutionState.Confident(candidates[0])
        isTikTok -> ShareResolutionState.ManualEntry(null, TIKTOK_NOTE)
        else -> ShareResolutionState.Ambiguous(candidates.take(3))
    }
