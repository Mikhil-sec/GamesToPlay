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

    /**
     * Words that follow a game name in a video title without being part of it.
     *
     * Used only by [continuesIntoTitleWord]: "Resident Evil Requiem Official Trailer" must not
     * read as though the title carries on past "Requiem", or the correct full match would be
     * demoted as a fragment. Deliberately excludes words that really do appear in titles —
     * "Remake", "Remaster", "Edition", "Definitive" — because demoting a match that stops short
     * of one of those is the right call.
     */
    private val TITLE_TRAILERS = setOf(
        "official", "trailer", "teaser", "reveal", "announcement", "announce", "cinematic",
        "walkthrough", "playthrough", "review", "reaction", "stream", "live", "guide", "tips",
        "speedrun", "montage", "highlights", "clip", "clips", "news", "update", "patch",
        "ranked", "explained", "everything", "breakdown", "impressions", "preview", "hands",
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

    /**
     * A token that reads as a sequel number rather than as prose or a year.
     *
     * One or two digits only, which is what keeps `"Elden Ring 2024 gameplay"` from being read
     * as a sequel while `"Resident Evil 9"` and `"Portal 2"` are. Roman numerals arrive here
     * already folded to digits by [normalize], so `"IV"` and `"4"` are the same token.
     */
    private val SEQUEL_NUMBER = Regex("^\\d{1,2}$")

    private fun isSequelNumber(normalizedToken: String): Boolean = SEQUEL_NUMBER.matches(normalizedToken)

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
            } else if (current.isNotEmpty() && isSequelNumber(normalize(bare))) {
                // A digit is not "capitalized", so every numbered sequel used to break its own
                // title in half here: "Resident Evil 4" produced the run "Resident Evil", and
                // "Resident Evil 9 Requiem" produced "Resident Evil" plus "Requiem" — so the
                // base game outranked the sequel the caption actually named (the second
                // 2026-08-23 closed-test report). Numbers continue a run; they never start one.
                current.add(bare)
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
     * What a match the caption itself says is incomplete gets multiplied by — see
     * [continuesIntoTitleWord].
     *
     * A *multiplier*, not a ceiling. A flat ceiling looked simpler and was wrong: in
     * "Resident Evil 4 Remake is amazing" both "Resident Evil" and "Resident Evil 4" are
     * incomplete matches, and clamping both to the same number threw away the one thing that
     * separates them — that the longer one is the better answer. [scoreOf] tops out at 1.0, so
     * multiplying by 0.84 keeps every penalised match under the 0.85 "Confident" bar and under
     * the 0.9 early-stop bar while preserving their order relative to each other.
     */
    const val FRAGMENT_PENALTY = 0.84f

    /**
     * What a match found only after ignoring a sequel number gets multiplied by.
     *
     * Deliberately below the "Confident" bar. "Resident Evil 9 Requiem" and "Mass Effect 2
     * Legendary Edition" are structurally identical — the caption carries a number the official
     * title doesn't — but in the first the user means Requiem and in the second they most
     * likely mean Mass Effect 2, and nothing in the string separates the two cases. A chooser
     * headed by the right game is the honest answer where a confident guess isn't.
     */
    private const val NUMBER_RELAXED_PENALTY = 0.84f

    /**
     * Confidence that `gameName` is really the game `originalText` is about — decided by
     * whether the name appears as **whole words** inside the original caption. Returns 0 when
     * it doesn't appear at all; callers should treat that as unverified.
     */
    fun verifyAgainstText(originalText: String, gameName: String): Float {
        val needle = normalize(gameName)
        if (needle.isEmpty()) return 0f

        val normalizedText = normalize(originalText)
        if (!" $normalizedText ".contains(" $needle ")) {
            // Second chance, ignoring a sequel number the caption carries and the official
            // title doesn't: IGDB calls it "Resident Evil Requiem", the internet calls it
            // "Resident Evil 9 Requiem", and whole-word containment sees no match at all
            // because of the digit sitting in the middle of the name.
            val relaxed = dropSequelNumbers(normalizedText)
            if (!" $relaxed ".contains(" $needle ")) return 0f
            return scoreOf(needle) * NUMBER_RELAXED_PENALTY
        }

        if (needle == normalizedText) return 1f

        val score = scoreOf(needle)
        return if (continuesIntoTitleWord(originalText, needle)) score * FRAGMENT_PENALTY else score
    }

    /** Longer, wordier names win — separates "Pal" from "Palworld" in a caption with both. */
    private fun scoreOf(needle: String): Float {
        val lengthScore = (needle.length / 18f).coerceAtMost(1f)
        val specificity = (words(needle).size / 3f).coerceAtMost(1f)
        return 0.75f + 0.15f * lengthScore + 0.1f * specificity
    }

    private fun dropSequelNumbers(normalizedText: String): String =
        words(normalizedText).filterNot { isSequelNumber(it) }.joinToString(" ")

    /**
     * True when the caption carries straight on from the matched name into another word that
     * looks like part of the same title.
     *
     * This is the fix for the closed-test report that "Resident Evil Requiem" matched to the
     * 1996 *Resident Evil*. Whole-word containment alone scores a strict prefix of a title
     * remarkably well — `"Resident Evil"` inside `"Resident Evil Requiem"` came out at 0.925,
     * over the 0.9 bar that stops the search early *and* over the 0.85 bar that presents a
     * result as confident — so the app confidently offered a thirty-year-old game for a caption
     * that plainly names a newer one. Nothing in the score noticed the leftover word.
     *
     * The signal is capitalisation in the **original** text, which is why this can't work off
     * the normalized string: a title word carries on in caps ("Resident Evil `Requiem`"), while
     * prose after a title does not ("Elden Ring `is` brutal"). Three things stop it firing on
     * ordinary video titles: a punctuation-only token between the two ("REQUIEM `-` Announcement
     * Trailer") reads as a subtitle boundary and ends the title; [STOPWORDS] cover prose; and
     * [TITLE_TRAILERS] cover the capitalised boilerplate that trails a name in a video title.
     * A following sequel *number* counts as a continuation too, since a digit is never
     * "capitalised" and so would otherwise slip through.
     *
     * Only the *following* word is examined, never the preceding one: a capital at the start of
     * a sentence is indistinguishable from a title word, so "Playing Hades tonight" would
     * otherwise demote a perfectly good match.
     */
    private fun continuesIntoTitleWord(originalText: String, normalizedNeedle: String): Boolean {
        val needleWords = words(normalizedNeedle)
        if (needleWords.isEmpty()) return false

        val raw = words(originalText)
        // Index the raw tokens by their normalized form, dropping the ones that normalize away
        // (bare "-", "|", emoji): those are exactly the separators that end a title.
        val normalizedTokens = raw.map { normalize(it) }

        var cursor = 0
        while (cursor < raw.size) {
            if (normalizedTokens[cursor].isEmpty()) { cursor++; continue }

            // Try to line the needle's words up starting here, allowing no gaps at all.
            var matched = 0
            var scan = cursor
            while (scan < raw.size && matched < needleWords.size && normalizedTokens[scan] == needleWords[matched]) {
                matched++
                scan++
            }

            if (matched == needleWords.size) {
                // `scan` is the token straight after the match — a separator there (or nothing)
                // means the title ended.
                val next = raw.getOrNull(scan) ?: return false
                val nextNormalized = normalizedTokens[scan]
                if (nextNormalized.isEmpty()) return false
                if (STOPWORDS.contains(nextNormalized) || TITLE_TRAILERS.contains(nextNormalized)) return false
                // A digit isn't capitalised but is unmistakably part of the title: "Resident
                // Evil" followed by "9" is not the game the caption names.
                if (isSequelNumber(nextNormalized)) return true
                return isCapitalized(next.replace(NON_NAME_CHAR, ""))
            }
            cursor++
        }
        return false
    }
}
