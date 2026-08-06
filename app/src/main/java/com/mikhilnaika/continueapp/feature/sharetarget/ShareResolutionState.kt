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
    data class ManualEntry(val prefillText: String?) : ShareResolutionState
    data class Added(val gameName: String) : ShareResolutionState
}

private const val CONFIDENCE_THRESHOLD = 0.85f

fun classify(resolvedTitle: String?, candidates: List<ResolveCandidateDto>, needsManualEntry: Boolean, rawText: String?): ShareResolutionState =
    when {
        needsManualEntry || candidates.isEmpty() -> ShareResolutionState.ManualEntry(rawText)
        candidates[0].confidence >= CONFIDENCE_THRESHOLD -> ShareResolutionState.Confident(candidates[0])
        else -> ShareResolutionState.Ambiguous(candidates.take(3))
    }
