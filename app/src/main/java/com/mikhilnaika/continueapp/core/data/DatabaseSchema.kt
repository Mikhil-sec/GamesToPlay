package com.mikhilnaika.continueapp.core.data

import androidx.room.migration.Migration

/**
 * The database's version number, and the upgrade path between versions.
 *
 * **What this is for, since it isn't obvious until it bites.** Room stamps a version number into
 * every device's SQLite file. When the app opens a database whose stored version is lower than
 * the one the code expects, Room looks for a [Migration] covering that gap — and if it can't
 * find one it **throws `IllegalStateException` and the app dies on launch**. Not for the
 * developer: for every user who already had the app installed.
 *
 * That failure is easy to ship because it is invisible in normal testing. A **fresh install**
 * creates the database at the new version and works perfectly. Only an **upgrade** — exactly
 * what every existing tester gets from Play — hits the missing migration. So the app can pass
 * every check on the bench and crash on 100% of real devices the moment it reaches the store.
 *
 * The app is one entity change away from that today: `AppDatabase` was built with no migrations
 * and no fallback. This release deliberately routed around it — every change went into DAO
 * projections and queries, which don't touch the schema — but the next column added to
 * `pile_entries` or `games` would have done it.
 *
 * ### What to do when you *do* need to change the schema
 *
 * 1. Add or change the `@Entity` field.
 * 2. Bump [VERSION].
 * 3. Add a [Migration] to [AppMigrations.ALL] with the SQL that turns the old table into the new
 *    one — usually a single `ALTER TABLE … ADD COLUMN`.
 * 4. Build once. Room writes `app/schemas/…/<VERSION>.json`; **commit it**, because that file is
 *    the record of what the old shape actually was.
 *
 * `AppMigrationsTest` fails the build if you do 1 and 2 without 3, which is the whole point of
 * keeping the version here rather than inline in the `@Database` annotation.
 *
 * ### What we deliberately do *not* do
 *
 * **`fallbackToDestructiveMigration()` is banned in this app.** It makes the crash go away by
 * deleting the database — that is, by throwing away the user's entire pile, every clear date,
 * every ranking. `PileEntryEntity` calls that data "precious, backed up" and it's the only thing
 * in the app that can't be re-fetched. A crash is a bug; silently erasing someone's library is
 * worse than a bug. Write the migration.
 */
object DatabaseSchema {
    /**
     * Bump this — never the number inside `@Database` — and add a matching [Migration] below.
     *
     * `const val` so it can be used as an annotation argument, which is what keeps the declared
     * version and the migration list in one file and unable to drift apart.
     */
    const val VERSION = 1
}

object AppMigrations {

    /**
     * Every migration, oldest first. Empty is correct at version 1: there is no earlier shape to
     * come from.
     *
     * The shape the first real one will take, for whoever gets there:
     *
     * ```kotlin
     * private val MIGRATION_1_2 = object : Migration(1, 2) {
     *     override fun migrate(db: SupportSQLiteDatabase) {
     *         db.execSQL("ALTER TABLE pile_entries ADD COLUMN ratingNote TEXT")
     *     }
     * }
     * ```
     *
     * Add columns as nullable, or with a `NOT NULL DEFAULT`, so existing rows stay valid —
     * SQLite cannot add a `NOT NULL` column without one.
     */
    val ALL: Array<Migration> = emptyArray()

    /**
     * True when [ALL] forms an unbroken chain from version 1 to [DatabaseSchema.VERSION].
     *
     * Kept next to the list rather than only in the test so the rule is legible where the
     * mistake would be made. Asserted by `AppMigrationsTest`.
     */
    fun coversEveryVersion(targetVersion: Int = DatabaseSchema.VERSION): Boolean {
        var reached = 1
        while (reached < targetVersion) {
            val next = ALL.firstOrNull { it.startVersion == reached } ?: return false
            reached = next.endVersion
        }
        return reached == targetVersion
    }
}
