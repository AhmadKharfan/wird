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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Migrates real databases and lets Room's own validator judge the result.
 *
 * Opening the database *is* the assertion for table shape: Room compares what it finds
 * against the exported schema and refuses to start on a mismatch. Index names it does
 * **not** check — measured, not assumed — so those are asserted here directly, and so is
 * any data a migration is meant to translate.
 */
class WirdMigrationTest {

    /** Identity hashes Room recorded for each version, from the exported schema JSON. */
    private val version1IdentityHash = "143101baa8c252ddd525cfa6f5cfb122"
    private val version2IdentityHash = "b47027428d3ebd522b7b1c305724144b"
    private val version3IdentityHash = "e08863f4ea16054e866a25386e450e11"

    private val databaseFile: File =
        File.createTempFile("wird-migration-", ".db").also { it.delete() }

    @AfterTest
    fun cleanUp() {
        databaseFile.delete()
    }

    private fun withConnection(block: (SQLiteConnection) -> Unit) {
        val connection = BundledSQLiteDriver().open(databaseFile.absolutePath)
        try {
            block(connection)
        } finally {
            connection.close()
        }
    }

    private fun SQLiteConnection.stampVersion(version: Int, identityHash: String) {
        execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, '$identityHash')")
        execSQL("PRAGMA user_version = $version")
    }

    private val createPrayerRecord =
        "CREATE TABLE IF NOT EXISTS `prayer_record` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
            "`prayer` TEXT NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))"

    /** The database exactly as version 1 left it, with one row of real data in it. */
    private fun createVersion1Database() = withConnection { connection ->
        connection.execSQL(createPrayerRecord)
        connection.stampVersion(1, version1IdentityHash)
        connection.execSQL(
            "INSERT INTO prayer_record (id, epochDay, prayer, status) " +
                "VALUES ('20706:FAJR', 20706, 'FAJR', 'ON_TIME')",
        )
    }

    /** The database as version 2 left it, carrying a habit and an entry worth keeping. */
    private fun createVersion2Database() = withConnection { connection ->
        connection.execSQL(createPrayerRecord)
        HABIT_LAYER_TABLES.forEach(connection::execSQL)
        connection.stampVersion(2, version2IdentityHash)
        connection.execSQL(
            "INSERT INTO habit (revisionId, habitId, name, kind, target, iconKey, sortOrder, " +
                "effectiveFromEpochDay, retiredOnEpochDay, updatedAt) " +
                "VALUES ('rev-1', 'duha', 'duha', 'BOOL', 1, 'duha', 0, 20700, NULL, 1)",
        )
        connection.execSQL(
            "INSERT INTO entry (id, habitId, epochDay, value, updatedAt) " +
                "VALUES ('entry-1', 'duha', 20706, 1, 1)",
        )
    }

    /** The database as version 3 left it, with a settings row saved under [privacyMode]. */
    private fun createVersion3Database(privacyMode: String) = withConnection { connection ->
        HABIT_LAYER_TABLES.forEach(connection::execSQL)
        connection.stampVersion(3, version3IdentityHash)
        connection.execSQL(
            "INSERT INTO settings (id, themeMode, numeralSystem, latitude, longitude, " +
                "calculationMethod, privacyMode, updatedAt) " +
                "VALUES (0, 'DARK', 'ARABIC_INDIC', NULL, NULL, 'UMM_AL_QURA', '$privacyMode', 1)",
        )
    }

    private fun openMigratedDatabase(): WirdDatabase =
        Room.databaseBuilder<WirdDatabase>(name = databaseFile.absolutePath)
            .addMigrations(*WIRD_MIGRATIONS)
            .setDriver(BundledSQLiteDriver())
            .build()

    /** Opens the database, which runs the migrations and validates the result, then closes it. */
    private suspend fun migrate() {
        val database = openMigratedDatabase()
        database.entryDao().observeDay(0).first()
        database.close()
    }

    private suspend fun migratedSettings(): SettingsEntity? {
        val database = openMigratedDatabase()
        val settings = database.settingsDao().get()
        database.close()
        return settings
    }

    private fun tableNames(): Set<String> {
        val tables = mutableSetOf<String>()
        withConnection { connection ->
            val statement = connection.prepare("SELECT name FROM sqlite_master WHERE type = 'table'")
            while (statement.step()) tables += statement.getText(0)
            statement.close()
        }
        return tables
    }

    // --- version 1 -----------------------------------------------------------------------

    @Test
    fun migratesAVersionOneDatabaseAllTheWayToTheCurrentVersion() = runTest {
        createVersion1Database()

        migrate()

        assertTrue(tableNames().containsAll(listOf("habit", "entry", "settings", "outbox")))
    }

    @Test
    fun dropsThePrayerRecordTableOnTheWayToVersionThree() = runTest {
        // The one migration that destroys data. Every row it removes predates the habit
        // model and is superseded rather than lost — the five prayers are a counter habit.
        createVersion1Database()

        migrate()

        assertFalse(tableNames().contains("prayer_record"))
    }

    // --- version 2 --------------------------------------------------------------------------

    @Test
    fun keepsHabitsAndEntriesWrittenAtVersionTwo() = runTest {
        createVersion2Database()

        val database = openMigratedDatabase()
        val habits = database.habitDao().observeActive().first()
        val entries = database.entryDao().observeDay(20706).first()
        database.close()

        assertEquals(listOf("duha"), habits.map { it.habitId })
        assertEquals(listOf("entry-1"), entries.map { it.id })
        assertEquals(1, entries.single().value)
    }

    // --- version 3: the privacy modes are renamed and a nickname appears ----------------------

    @Test
    fun addsANicknameThatStartsEmpty() = runTest {
        createVersion3Database(privacyMode = "PRIVATE")

        assertNull(migratedSettings()?.nickname)
    }

    @Test
    fun keepsAPrivateChoicePrivate() = runTest {
        // The one choice that must never be loosened by an upgrade.
        createVersion3Database(privacyMode = "PRIVATE")

        assertEquals("PRIVATE", migratedSettings()?.privacyMode)
    }

    @Test
    fun translatesTheOldAnonymousModeToPointsOnly() = runTest {
        // Anonymous meant "my points, not my name" — which is exactly points-only.
        createVersion3Database(privacyMode = "ANONYMOUS")

        assertEquals("POINTS_ONLY", migratedSettings()?.privacyMode)
    }

    @Test
    fun translatesTheOldOpenModeToPointsOnlyRatherThanToANickname() = runTest {
        // Open shared a name, and its nearest new mode is the nickname — but no nickname
        // exists to share, so points-only is the translation that shows no more than the
        // user already agreed to.
        createVersion3Database(privacyMode = "OPEN")

        assertEquals("POINTS_ONLY", migratedSettings()?.privacyMode)
    }

    @Test
    fun keepsTheOtherSettingsOfAMigratedRow() = runTest {
        createVersion3Database(privacyMode = "OPEN")

        val settings = migratedSettings()
        assertEquals("DARK", settings?.themeMode)
        assertEquals("ARABIC_INDIC", settings?.numeralSystem)
    }

    // --- the result ---------------------------------------------------------------------------

    @Test
    fun recordsTheNewVersionOnDisk() = runTest {
        createVersion1Database()

        migrate()

        var version = 0
        withConnection { connection ->
            val statement = connection.prepare("PRAGMA user_version")
            statement.step()
            version = statement.getInt(0)
            statement.close()
        }

        assertEquals(4, version)
    }

    @Test
    fun createsEveryIndexTheSchemaDeclares() = runTest {
        // Room's own validation does NOT check index names: renaming one opens cleanly and
        // silently costs the query planner the index. Measured, not assumed.
        createVersion1Database()

        migrate()

        val indices = mutableSetOf<String>()
        withConnection { connection ->
            val statement = connection.prepare(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name NOT LIKE 'sqlite_%'",
            )
            while (statement.step()) indices += statement.getText(0)
            statement.close()
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

    private companion object {
        /**
         * The four habit-layer tables exactly as versions 2 and 3 created them, copied from
         * the exported schema. Version 2 had these plus `prayer_record`; version 3 had only
         * these.
         */
        val HABIT_LAYER_TABLES = listOf(
            "CREATE TABLE IF NOT EXISTS `habit` (`revisionId` TEXT NOT NULL, `habitId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `kind` TEXT NOT NULL, `target` INTEGER NOT NULL, " +
                "`iconKey` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, " +
                "`effectiveFromEpochDay` INTEGER NOT NULL, `retiredOnEpochDay` INTEGER, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`revisionId`))",
            "CREATE INDEX IF NOT EXISTS `index_habit_habitId` ON `habit` (`habitId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_habitId_effectiveFromEpochDay` " +
                "ON `habit` (`habitId`, `effectiveFromEpochDay`)",
            "CREATE TABLE IF NOT EXISTS `entry` (`id` TEXT NOT NULL, `habitId` TEXT NOT NULL, " +
                "`epochDay` INTEGER NOT NULL, `value` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_entry_habitId_epochDay` " +
                "ON `entry` (`habitId`, `epochDay`)",
            "CREATE INDEX IF NOT EXISTS `index_entry_epochDay` ON `entry` (`epochDay`)",
            "CREATE TABLE IF NOT EXISTS `settings` (`id` INTEGER NOT NULL, `themeMode` TEXT NOT NULL, " +
                "`numeralSystem` TEXT NOT NULL, `latitude` REAL, `longitude` REAL, " +
                "`calculationMethod` TEXT NOT NULL, `privacyMode` TEXT NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `outbox` (`id` TEXT NOT NULL, `entityType` TEXT NOT NULL, " +
                "`entityId` TEXT NOT NULL, `op` TEXT NOT NULL, `payload` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_outbox_createdAt` ON `outbox` (`createdAt`)",
        )
    }
}
