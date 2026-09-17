package com.mikhilnaika.continueapp.core.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The guard that turns "the app crashes on launch for everyone who upgrades" into "the build
 * fails".
 *
 * A missing Room migration is invisible on the bench — a fresh install creates the database at
 * the new version and works perfectly, and only an *upgrade* (what every tester gets from Play)
 * hits it. So the mistake has to be caught somewhere that runs before the APK exists. That's
 * here.
 *
 * Reads the real exported schemas from `app/schemas/`, the same way `OfflineGameIndexTest` reads
 * the real shipped index: a fixture would only prove the loop works.
 */
class AppMigrationsTest {

    /** Gradle runs the `app` module's unit tests with the module root as the working directory. */
    private val schemaDir = File("schemas/com.mikhilnaika.continueapp.core.data.AppDatabase")

    private fun exportedVersions(): List<Int> {
        assertTrue(
            "Expected ${schemaDir.absolutePath} to exist. Room writes it on every build because " +
                "AppDatabase sets exportSchema = true — if it's gone, that was turned off.",
            schemaDir.isDirectory,
        )
        return schemaDir.listFiles()
            .orEmpty()
            .mapNotNull { it.name.removeSuffix(".json").toIntOrNull() }
            .sorted()
    }

    @Test
    fun `the declared version matches the newest exported schema`() {
        // If this fails, someone changed an @Entity and let Room export a new schema without
        // bumping DatabaseSchema.VERSION — or bumped it and didn't rebuild.
        val newest = exportedVersions().maxOrNull()
        assertEquals(
            "DatabaseSchema.VERSION is ${DatabaseSchema.VERSION} but the newest exported schema " +
                "is $newest. Bump the version, add the migration, rebuild, and commit the new " +
                "schema JSON — see the doc on DatabaseSchema.",
            DatabaseSchema.VERSION,
            newest,
        )
    }

    @Test
    fun `every version has a migration path from version 1`() {
        assertTrue(
            "AppMigrations.ALL does not form an unbroken chain from 1 to " +
                "${DatabaseSchema.VERSION}. Every user upgrading from an older build will hit " +
                "IllegalStateException on launch. Add the missing Migration — and do not reach " +
                "for fallbackToDestructiveMigration(), which fixes the crash by deleting the " +
                "user's pile.",
            AppMigrations.coversEveryVersion(),
        )
    }

    @Test
    fun `no migration is declared backwards or in place`() {
        AppMigrations.ALL.forEach { migration ->
            assertTrue(
                "Migration ${migration.startVersion} -> ${migration.endVersion} does not move " +
                    "forwards.",
                migration.endVersion > migration.startVersion,
            )
        }
    }

    @Test
    fun `every exported schema older than the current one is still committed`() {
        // The old JSON is the only record of what the previous table shape actually was, which
        // is what a future migration has to be written against. Losing one is unrecoverable.
        val versions = exportedVersions()
        assertEquals(
            "Exported schemas are $versions — expected every version from 1 to " +
                "${DatabaseSchema.VERSION}. A deleted schema JSON can't be regenerated.",
            (1..DatabaseSchema.VERSION).toList(),
            versions,
        )
    }

    /**
     * The migration must build exactly the tables Room expects, or Room refuses to open the
     * database — for upgrading users only, since a fresh install never runs the migration.
     * So the SQL is checked against Room's own export, not against itself.
     */
    @Test
    fun `migration 1 to 2 builds the friend tables exactly as Room generates them`() {
        val v1 = File(schemaDir, "1.json").readText()
        val v2 = Json.parseToJsonElement(File(schemaDir, "2.json").readText()).jsonObject
        val entities = v2.getValue("database").jsonObject.getValue("entities").jsonArray.map { it.jsonObject }
        val added = entities.filter { "\"${it.string("tableName")}\"" !in v1 }
        assertEquals(setOf("friends", "friend_games", "friend_ranks"), added.map { it.string("tableName") }.toSet())

        val expected = added.flatMap { entity ->
            val table = entity.string("tableName")
            val indices = entity["indices"]?.jsonArray.orEmpty().map { it.jsonObject.string("createSql") }
            (listOf(entity.string("createSql")) + indices).map { it.replace("\${TABLE_NAME}", table) }
        }
        assertEquals(expected.toSet(), AppMigrations.MIGRATION_1_2_SQL.toSet())
    }

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
}
