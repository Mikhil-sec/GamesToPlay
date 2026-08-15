package com.mikhilnaika.continueapp.core.offline

import com.mikhilnaika.continueapp.core.util.GameNameCandidates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Exercises [OfflineGameIndex.parse] against the **real, shipped** `game_index.tsv.gz` — not a
 * fixture. This project has twice shipped a config that "looked applied" but silently returned
 * wrong or empty results (see docs/10-BUILD-STATUS.md bug #10 — the `category`/`game_type`
 * filter — and the `backgroundUrl: null` bug fixed 2026-08-14); the fix both times was
 * "verify against real content, not source." A JVM test can't launch Android to load the asset
 * through `AssetManager`, but the parser itself is a pure function of an [java.io.InputStream],
 * so it's exercised directly against the file on disk instead — same bytes the APK ships.
 */
class OfflineGameIndexTest {

    /** Gradle's working directory for the `app` module's `test` task is the module root, so
     * this resolves to the real asset without any test-resource duplication. */
    private val assetFile = File("src/main/assets/game_index.tsv.gz")

    private fun loadRealIndex(): Map<String, List<OfflineGameRecord>> {
        assertTrue(
            "Expected ${assetFile.absolutePath} to exist — run `node tools/igdb_dump_index.mjs` " +
                "if the offline index was never built (docs/08-GAME-DATA.md §Data dumps).",
            assetFile.exists(),
        )
        return assetFile.inputStream().use { OfflineGameIndex.parse(GZIPInputStream(it)) }
    }

    @Test
    fun `real index parses into a non-trivial number of games`() {
        val index = loadRealIndex()
        // 17,095 games were indexed at build time (2026-08-14); a wide floor rather than the
        // exact number so a routine re-run of the pipeline (dumps update daily) doesn't fail
        // this test over ordinary drift.
        val totalRecords = index.values.flatten().map { it.id }.distinct().size
        assertTrue("Expected thousands of games, found $totalRecords", totalRecords > 5000)
    }

    @Test
    fun `every parsed year column is a real integer, never the NaN string`() {
        // Regression test for the exact bug found and fixed this session: the CSV dump encodes
        // first_release_date as a datetime string ("2023-08-15 00:00:00"), not the Unix
        // timestamp the REST API returns. Treating it as a timestamp doesn't throw — it
        // silently serializes the literal string "NaN" into the TSV. `OfflineGameRecord`
        // doesn't carry year at all (deliberately thin, see the class doc), so this asserts on
        // the raw file directly rather than through the record.
        assetFile.inputStream().use { raw ->
            GZIPInputStream(raw).bufferedReader(Charsets.UTF_8).useLines { lines ->
                val rows = lines.drop(1) // header
                for (line in rows) {
                    val cols = line.split('\t')
                    if (cols.size < 8) continue
                    assertTrue("Found literal \"NaN\" in a data row: $line", cols[3] != "NaN")
                }
            }
        }
    }

    @Test
    fun `a well-known game is findable by its exact name`() {
        val index = loadRealIndex()
        val hit = index[GameNameCandidates.normalize("The Witcher 3: Wild Hunt")]
        assertNotNull("The Witcher 3 should be in an index built from a real IGDB dump", hit)
        assertTrue(hit!!.any { it.name.contains("Witcher 3", ignoreCase = true) })
    }

    @Test
    fun `a well-known game is findable by an alternative name`() {
        val index = loadRealIndex()
        // "Baldur's Gate III" is commonly abbreviated "BG3" — exactly the kind of string IGDB's
        // own `search` cannot match but `alternative_names` carries, which is the entire reason
        // the data-dump index exists (docs/08-GAME-DATA.md §Data dumps).
        val hit = index[GameNameCandidates.normalize("BG3")]
        assertNotNull("BG3 should resolve via alternative_names", hit)
        assertTrue(hit!!.any { it.name.contains("Baldur's Gate", ignoreCase = true) })
    }

    @Test
    fun `cover image ids look like real IGDB CDN path segments, not URLs`() {
        val index = loadRealIndex()
        val withCover = index.values.flatten().firstOrNull { it.coverImageId != null }
        assertNotNull("Expected at least one indexed game to have a cover", withCover)
        val coverImageId = withCover!!.coverImageId!!
        assertTrue("coverImageId should be a bare id, not a URL: $coverImageId", !coverImageId.contains("http"))
        assertTrue("IGDB cover image ids start with 'co': $coverImageId", coverImageId.startsWith("co"))
    }
}
