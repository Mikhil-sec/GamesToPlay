package com.mikhilnaika.continueapp.core.util

/**
 * Pure, unit-tested function that turns noisy shared text into ranked candidate game-title
 * strings — docs/02-PRODUCT-SPEC.md §2a Stage 2 and docs/05-TECH-ARCHITECTURE.md's
 * "the one piece of logic where correctness is directly visible to judges."
 *
 * This function does NOT match against IGDB — it only cleans and ranks candidate strings.
 * Actual fuzzy matching (e.g. "eldenring" -> "Elden Ring") happens server-side against IGDB
 * search, which is where that kind of dictionary-free word-segmentation problem belongs.
 *
 * Noise-stripping rules are kept in one table ([NOISE_PATTERNS]) so they can be tuned
 * without touching the pipeline logic, per the doc's explicit instruction.
 */
object TitleParser {

    /** Boilerplate / noise regexes, applied in order, case-insensitive. */
    private val NOISE_PATTERNS: List<Regex> = listOf(
        Regex("""https?://\S+"""),                 // URLs
        Regex("""www\.\S+"""),
        // Scheme-less links — share sheets hand over "youtu.be/abc" and "instagram.com/reel/xyz"
        // at least as often as full URLs. Without this they survive cleaning and get offered to
        // the user as a suggested game title, which they then have to delete before typing.
        Regex("""[\w-]+(\.[\w-]+)+/\S*"""),
        Regex("""\[[^\]]*]"""),                     // [4K], [HD]
        Regex("""\([^)]*\)"""),                     // (Official Trailer)
        Regex("""\br/\w+"""),                       // r/gaming
        Regex("""@\w+"""),                          // @handles
        Regex("""\bPART\s*\d+\b""", RegexOption.IGNORE_CASE),
        Regex("""\bEP\.?\s*\d+\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(REVIEW|GAMEPLAY|LET'?S PLAY|WALKTHROUGH|FULL GAME|OFFICIAL TRAILER|TRAILER)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(19|20)\d{2}\b"""),               // trailing years
        Regex("""[\p{So}\p{Cn}]"""),                 // emoji / symbol-other codepoints
    )

    private val HASHTAG = Regex("""#(\w+)""")
    private val CAMEL_BOUNDARY = Regex("""(?<=[a-z0-9])(?=[A-Z])""")
    private val WHITESPACE = Regex("""\s+""")

    /**
     * @return candidate title strings, strongest signal first (hashtags), then the cleaned
     * full text as a fallback candidate. Never throws; returns an empty list for blank input
     * so callers can fall through to manual search (docs/02-PRODUCT-SPEC.md degradation ladder).
     */
    fun extractCandidates(rawText: String?): List<String> {
        if (rawText.isNullOrBlank()) return emptyList()

        val hashtagCandidates = HASHTAG.findAll(rawText)
            .map { unCamelAndClean(it.groupValues[1]) }
            .filter { it.length >= 2 }
            .toList()

        val withoutHashtags = HASHTAG.replace(rawText, "")
        val cleanedFullText = stripNoise(withoutHashtags)

        val all = LinkedHashSet<String>()
        all.addAll(hashtagCandidates)
        if (cleanedFullText.length >= 2) all.add(cleanedFullText)

        return all.toList()
    }

    private fun unCamelAndClean(hashtag: String): String {
        val spaced = CAMEL_BOUNDARY.replace(hashtag, " ")
        return spaced.lowercase().trim()
    }

    private fun stripNoise(text: String): String {
        var result = text
        for (pattern in NOISE_PATTERNS) {
            result = pattern.replace(result, " ")
        }
        // Strip everything after the first '|' — common video-title separator.
        result = result.substringBefore('|')
        result = WHITESPACE.replace(result, " ").trim()
        return result
    }
}
