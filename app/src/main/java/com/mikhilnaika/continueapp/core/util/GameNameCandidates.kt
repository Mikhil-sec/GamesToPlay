package com.mikhilnaika.continueapp.core.util

/**
 * Kotlin port of `worker/src/resolve/candidates.ts` — turns a noisy caption into an ordered
 * shortlist of strings worth matching, and scores a matched name against the original text.
 *
 * Unlike [TitleParser] (which mirrors `titleParser.ts` and was deliberately left unchanged),
 * this file has no prior Kotlin counterpart: `candidates.ts`'s ranking/verification logic only
 * ever ran server-side, because matching meant an IGDB search call. `OfflineGameIndex` (added
 * 2026-08-14 alongside the IGDB data-dump pipeline, docs/08-GAME-DATA.md §Data dumps) replaces
 * that search with a local dictionary lookup, which is what makes running this on-device worth
 * doing — it turns share matching into something that works with no network at all.
 *
 * Kept behaviourally identical to the TypeScript original on purpose: the two need to agree on
 * what candidate strings are worth trying and how confident a match is, or the offline and
 * online paths would silently disagree about the same caption.
 */
object GameNameCandidates {

    /** Stop searching further candidates once a hit is at least this convincing. */
    const val CONFIDENT_ENOUGH = 0.9f

    /** Lowercase words that legitimately appear *inside* a title and shouldn't break a run. */
    private val CONNECTORS = setOf("of", "the", "and", "a", "an", "in", "to", "de", "la", "at", "for", "vs")

    /** Words never worth searching on their own — common caption filler, not game names. */
    private val STOPWORDS = CONNECTORS + setOf(
        "i", "my", "me", "we", "you", "your", "he", "she", "it", "they", "this", "that", "these",
        "is", "was", "are", "were", "be", "been", "has", "have", "had", "do", "does", "did", "get",
        "got", "go", "goes", "went", "can", "will", "just", "new", "best", "worst", "how", "why",
        "what", "when", "where", "who", "all", "own", "out", "up", "on", "off", "with", "from",
        "their", "there", "his", "her", "its", "more", "than", "then", "now", "not", "but",
        "so", "if", "or", "as", "by", "each", "method", "items",
        "gameplay", "part", "ep", "episode", "shorts", "short", "video", "funny", "moments",
    )

    private const val MAX_WORDS_IN_CANDIDATE = 6

    /** Sequels are written both ways in the wild — IGDB says "III", captions say "3". Only II+
     * is folded; a bare "I" is far more often the pronoun than a sequel number. */
    private val ROMAN_NUMERALS = mapOf(
        "ii" to "2", "iii" to "3", "iv" to "4", "v" to "5", "vi" to "6",
        "vii" to "7", "viii" to "8", "ix" to "9", "x" to "10",
    )

    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
    private val NON_NAME_CHAR = Regex("[^\\p{L}\\p{N}':-]")

    private fun words(text: String): List<String> = text.split(Regex("\\s+")).filter { it.isNotEmpty() }

    /** `"Elden  Ring!"` -> `"elden ring"`. Used for every comparison so punctuation never matters. */
    fun normalize(text: String): String =
        text.lowercase()
            .replace(NON_WORD, " ")
            .trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { ROMAN_NUMERALS[it] ?: it }

    private fun isCapitalized(word: String): Boolean {
        val first = word.firstOrNull() ?: return false
        return first.isUpperCase()
    }

    /** Runs of capitalized words, allowing lowercase connectors in the middle — pulls
     * `"League of Legends"` out of `"a tale of two bush ganks League of Legends"`. */
    private fun properNounRuns(text: String): List<String> {
        val runs = mutableListOf<String>()
        var current = mutableListOf<String>()

        for (word in words(text)) {
            val bare = word.replace(NON_NAME_CHAR, "")
            if (bare.isEmpty()) continue

            if (isCapitalized(bare)) {
                current.add(bare)
            } else if (current.isNotEmpty() && CONNECTORS.contains(bare.lowercase())) {
                current.add(bare) // keep "of" inside "League of Legends"
            } else {
                if (current.isNotEmpty()) runs.add(current.joinToString(" "))
                current = mutableListOf()
            }
        }
        if (current.isNotEmpty()) runs.add(current.joinToString(" "))

        // A run can't usefully end on a connector ("Rise of" -> "Rise").
        return runs.map { run ->
            val parts = words(run).toMutableList()
            while (parts.isNotEmpty() && CONNECTORS.contains(parts.last().lowercase())) parts.removeAt(parts.size - 1)
            parts.joinToString(" ")
        }.filter { it.length >= 2 }
    }

    /** Every contiguous word window inside a phrase, longest first — `"Pocketpair Palworld"`
     * is one proper-noun run, and only its second word is the actual game. */
    private fun windows(phrase: String): List<String> {
        val parts = words(phrase)
        val out = mutableListOf<String>()
        for (size in minOf(parts.size, MAX_WORDS_IN_CANDIDATE) downTo 1) {
            for (start in 0..(parts.size - size)) {
                val window = parts.subList(start, start + size).joinToString(" ")
                if (size == 1 && STOPWORDS.contains(window.lowercase())) continue
                if (window.length >= 2) out.add(window)
            }
        }
        return out
    }

    /**
     * Ordered, de-duplicated shortlist of strings to try matching, likeliest first:
     * hashtags, whole proper-noun runs, windows inside those runs, trailing windows of the
     * caption, then the cleaned caption itself as a last resort.
     */
    fun rankedCandidates(rawText: String?): List<String> {
        if (rawText.isNullOrBlank()) return emptyList()

        val ordered = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        fun push(candidate: String) {
            val cleaned = candidate.trim()
            val key = normalize(cleaned)
            if (key.length < 2 || seen.contains(key)) return
            seen.add(key)
            ordered.add(cleaned)
        }

        val base = TitleParser.extractCandidates(rawText) // hashtags first, then cleaned full text
        val cleanedFullText = base.lastOrNull() ?: rawText
        base.dropLast(1).forEach { push(it) }

        val runs = properNounRuns(cleanedFullText).sortedByDescending { words(it).size }
        runs.forEach { push(it) }
        runs.forEach { run -> windows(run).forEach { push(it) } }

        val allWords = words(cleanedFullText)
        for (size in minOf(4, allWords.size) downTo 2) {
            push(allWords.takeLast(size).joinToString(" "))
        }

        push(cleanedFullText)
        return ordered
    }

    /**
     * Confidence that `gameName` is really the game `originalText` is about — decided by
     * whether the name appears as **whole words** inside the original caption. Returns 0 when
     * it doesn't appear at all; callers should treat that as unverified.
     */
    fun verifyAgainstText(originalText: String, gameName: String): Float {
        val haystack = " ${normalize(originalText)} "
        val needle = normalize(gameName)
        if (needle.isEmpty()) return 0f
        if (!haystack.contains(" $needle ")) return 0f

        if (needle == normalize(originalText)) return 1f

        // Longer names win — separates "Pal" from "Palworld" when a caption contains both.
        val lengthScore = (needle.length / 18f).coerceAtMost(1f)
        val specificity = (words(needle).size / 3f).coerceAtMost(1f)
        return 0.75f + 0.15f * lengthScore + 0.1f * specificity
    }
}
