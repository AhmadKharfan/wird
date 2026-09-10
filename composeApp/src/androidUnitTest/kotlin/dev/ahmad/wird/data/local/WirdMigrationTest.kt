package dev.ahmad.wird.data.local

import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Migrates a real version-1 database and lets Room's own validator judge the result.
 *
 * This is the point of the test: Room compares the migrated database against the exported
 * `schemas/2.json` when it opens and refuses to start on any difference, down to an index
 * name. So a migration whose SQL merely looks equivalent fails here rather than on a
 * user's device. Nothing below asserts the schema by hand — opening the database *is* the
 * assertion.
 */
class WirdMigrationTest {

    /** The identity hash Room recorded for version 1, from `schemas/1.json`. */
    private val version1IdentityHash = "143101baa8c252ddd525cfa6f5cfb122"

    private val databaseFile: File =
        File.createTempFile("wird-migration-", ".db").also { it.delete() }

    @AfterTest
    fun cleanUp() {
        databaseFile.delete()
    }

    /** Builds the database exactly as version 1 left it, with one row of real data in it. */
    private fun createVersion1Database() {
        val connection: SQLiteConnection = BundledSQLiteDriver().open(databaseFile.absolutePath)
        try {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `prayer_record` (`id` TEXT NOT NULL, " +
                    "`epochDay` INTEGER NOT NULL, `prayer` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table " +
                    "(id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            connection.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
                    "VALUES(42, '$version1IdentityHash')",
            )
            connection.execSQL("PRAGMA user_version = 1")
            connection.execSQL(
                "INSERT INTO prayer_record (id, epochDay, prayer, status) " +
                    "VALUES ('20706:FAJR', 20706, 'FAJR', 'ON_TIME')",
            )
        } finally {
            connection.close()
        }
    }

    private fun openMigratedDatabase(): WirdDatabase =
        Room.databaseBuilder<WirdDatabase>(name = databaseFile.absolutePath)
            .addMigrations(*WIRD_MIGRATIONS)
            .setDriver(BundledSQLiteDriver())
            .build()

    @Test
    fun migratesAVersionOneDatabaseWithoutLosingItsRows() = runTest {
        createVersion1Database()

        val database = openMigratedDatabase()
        // Forces the open, which runs MIGRATION_1_2 and then validates against schema 2.
        val records = database.prayerRecordDao().observeByDay(20706).first()
        database.close()

        assertEquals(1, records.size)
        assertEquals("FAJR", records.single().prayer)
        assertEquals("ON_TIME", records.single().status)
    }

    @Test
    fun leavesTheNewTablesReadyToUse() = runTest {
        createVersion1Database()

        val database = openMigratedDatabase()
        database.prayerRecordDao().observeByDay(20706).first()
        database.close()

        val connection = BundledSQLiteDriver().open(databaseFile.absolutePath)
        val tables = mutableSetOf<String>()
        try {
            val statement = connection.prepare("SELECT name FROM sqlite_master WHERE type = 'table'")
            while (statement.step()) tables += statement.getText(0)
            statement.close()
        } finally {
            connection.close()
        }

        assertTrue(tables.containsAll(listOf("habit", "entry", "settings", "outbox")))
        // Superseded but not yet dropped: it goes in version 3, with the code that reads it.
        assertTrue(tables.contains("prayer_record"))
    }

    @Test
    fun createsEveryIndexTheSchemaDeclares() = runTest {
        // Room's own validation does NOT check index names: renaming one opens cleanly and
        // silently costs the query planner the index. Measured, not assumed — so the names
        // are asserted here instead.
        createVersion1Database()

        val database = openMigratedDatabase()
        database.prayerRecordDao().observeByDay(20706).first()
        database.close()

        val connection = BundledSQLiteDriver().open(databaseFile.absolutePath)
        val indices = mutableSetOf<String>()
        try {
            val statement = connection.prepare(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name NOT LIKE 'sqlite_%'",
            )
            while (statement.step()) indices += statement.getText(0)
            statement.close()
        } finally {
            connection.close()
        }

        assertEquals(
            setOf(
                "index_habit_habitId",
                "index_habit_habitId_effectiveFromEpochDay",
                "index_entry_habitId_epochDay",
                "index_entry_epochDay",
                "index_outbox_createdAt",
            ),
            indices,
        )
    }

    @Test
    fun recordsTheNewVersionOnDisk() = runTest {
        createVersion1Database()

        val database = openMigratedDatabase()
        database.prayerRecordDao().observeByDay(20706).first()
        database.close()

        val connection = BundledSQLiteDriver().open(databaseFile.absolutePath)
        val version: Int
        try {
            val statement = connection.prepare("PRAGMA user_version")
            statement.step()
            version = statement.getInt(0)
            statement.close()
        } finally {
            connection.close()
        }

        assertEquals(2, version)
    }
}
