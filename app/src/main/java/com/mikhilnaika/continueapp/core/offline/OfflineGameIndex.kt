package com.mikhilnaika.continueapp.core.offline

import android.content.Context
import com.mikhilnaika.continueapp.core.network.dto.ResolveCandidateDto
import com.mikhilnaika.continueapp.core.util.GameNameCandidates
import com.mikhilnaika.continueapp.core.util.IgdbImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device name -> IGDB id lookup, built offline from IGDB's data dumps
 * (docs/08-GAME-DATA.md §Data dumps, `tools/igdb_dump_index.mjs`) and shipped as an app asset.
 *
 * This is what makes share matching **structurally** accurate rather than best-effort. Before
 * this existed, matching a caption meant firing candidate substrings at IGDB's `search`, which
 * is near-exact — one extra word returns zero results — so `/resolve` tried up to 6 guesses and
 * hoped one matched. With the real name table (plus every `alternative_names` row — "BG3",
 * "FF7", regional titles) on-device, matching a candidate is an exact dictionary lookup instead
 * of a guess, it costs no network round trip, and it works with the phone in airplane mode.
 *
 * Loading is lazy and memoized: parsing ~41k rows of gzipped TSV is real work (sub-second, but
 * not free), so it starts once at construction — [ContinueApplication] injects this eagerly at
 * app startup, the same warm-up pattern already used for [com.mikhilnaika.continueapp.core.data.SeedLoader] —
 * and every call after that awaits the same [Deferred].
 */
@Singleton
class OfflineGameIndex @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val assets = context.assets
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Normalized name (and every alternative name) -> matching records, ratingCount desc. */
    private val indexDeferred: Deferred<Map<String, List<OfflineGameRecord>>> = scope.async {
        // A missing or corrupt asset must degrade to "no offline match", never crash the share
        // sheet — the network path is a complete fallback on its own.
        runCatching { load() }.getOrElse { emptyMap() }
    }

    private fun load(): Map<String, List<OfflineGameRecord>> =
        assets.open(ASSET_NAME).use { raw -> parse(GZIPInputStream(raw)) }

    /** Explicit warm-up hook for [com.mikhilnaika.continueapp.ContinueApplication] — the index
     * is already loading by construction, this just lets startup wait for it if it wants to. */
    suspend fun warmUp() {
        indexDeferred.await()
    }

    /**
     * Mirrors `matchCandidates()` in `worker/src/resolve/resolveGame.ts`: try ranked candidate
     * strings in order, score every hit against the **original** text (never the candidate that
     * found it — that's what makes a one-word guess like "Palworld" safe), stop once a hit is
     * confident enough. The only structural difference from the server version is that "search"
     * here is an exact dictionary lookup instead of an IGDB API call, because the whole
     * dictionary is on-device.
     */
    suspend fun match(rawText: String?): List<ResolveCandidateDto> {
        if (rawText.isNullOrBlank()) return emptyList()
        val index = indexDeferred.await()
        if (index.isEmpty()) return emptyList()

        val scored = LinkedHashMap<Long, ResolveCandidateDto>()
        var best = 0f

        for (candidate in GameNameCandidates.rankedCandidates(rawText).take(MAX_CANDIDATES_TRIED)) {
            val records = index[GameNameCandidates.normalize(candidate)] ?: continue
            for (record in records) {
                val confidence = GameNameCandidates.verifyAgainstText(rawText, record.name)
                if (confidence <= 0f) continue
                val existing = scored[record.id]
                if (existing == null || existing.confidence < confidence) {
                    scored[record.id] = ResolveCandidateDto(
                        id = record.id,
                        name = record.name,
                        confidence = confidence,
                        coverUrl = record.coverImageId?.let { IgdbImage.coverUrl(it) },
                    )
                }
                if (confidence > best) best = confidence
            }
            if (best >= GameNameCandidates.CONFIDENT_ENOUGH) break
        }
        return scored.values.sortedByDescending { it.confidence }.take(5)
    }

    companion object {
        private const val ASSET_NAME = "game_index.tsv.gz"

        /** A local lookup is far cheaper than an IGDB request, but a pathological caption can
         * still generate a long candidate list (see `GameNameCandidates.windows`) — capped so
         * one share can't spend unbounded CPU on the main app process. */
        private const val MAX_CANDIDATES_TRIED = 24

        private const val EXPECTED_COLUMNS = 8

        /**
         * Pure parse, factored out so it can be exercised directly against the real shipped
         * asset file from a plain JVM test (`OfflineGameIndexTest`) with no Android dependency.
         */
        internal fun parse(input: InputStream): Map<String, List<OfflineGameRecord>> {
            val map = HashMap<String, MutableList<OfflineGameRecord>>()

            fun index(key: String, record: OfflineGameRecord) {
                val normalized = GameNameCandidates.normalize(key)
                if (normalized.isEmpty()) return
                map.getOrPut(normalized) { mutableListOf() }.add(record)
            }

            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
                val iterator = lines.iterator()
                if (!iterator.hasNext()) return map
                iterator.next() // header: id, name, cover, year, rating, ratingCount, hours, alt
                while (iterator.hasNext()) {
                    val cols = iterator.next().split('\t')
                    if (cols.size < EXPECTED_COLUMNS) continue
                    val id = cols[0].toLongOrNull() ?: continue
                    val name = cols[1]
                    if (name.isEmpty()) continue
                    val cover = cols[2].ifBlank { null }
                    val ratingCount = cols[5].toIntOrNull() ?: 0
                    val alt = cols[7]

                    val record = OfflineGameRecord(id, name, cover, ratingCount)
                    index(name, record)
                    if (alt.isNotBlank()) {
                        for (altName in alt.split('|')) {
                            if (altName.isNotBlank()) index(altName, record)
                        }
                    }
                }
            }

            // Highest-rated first within a bucket, so a normalized-name collision (a remaster
            // sharing its base title) still prefers the more likely game before confidence
            // scoring — which can't distinguish them, since both names verify identically.
            for (key in map.keys) map.getValue(key).sortByDescending { it.ratingCount }
            return map
        }
    }
}
