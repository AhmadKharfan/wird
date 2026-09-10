package dev.ahmad.wird.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Every migration this database has ever had.
//
// There is no `fallbackToDestructiveMigration` anywhere in this project. A user's practice
// log is the whole point of the app, so a schema change that cannot be migrated is a bug to
// fix, not data to discard.
//
// Table statements are copied verbatim from the generated schema JSON rather than written by
// hand. Room validates the migrated database against that schema on open — columns, types,
// nullability — and refuses to start on a difference. It does NOT check index names, which
// was measured rather than assumed, so WirdMigrationTest asserts those separately.

/**
 * Adds the habit, entry, settings and outbox tables.
 *
 * `prayer_record` is deliberately left alone. It is superseded by habit/entry, but the code
 * that read it was still live at this version; it is dropped in the migration that landed
 * with its removal, so no version in between had a database the app could not open.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `habit` (`revisionId` TEXT NOT NULL, `habitId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `kind` TEXT NOT NULL, `target` INTEGER NOT NULL, " +
                "`iconKey` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, " +
                "`effectiveFromEpochDay` INTEGER NOT NULL, `retiredOnEpochDay` INTEGER, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`revisionId`))",
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_habitId` ON `habit` (`habitId`)")
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_habitId_effectiveFromEpochDay` " +
                "ON `habit` (`habitId`, `effectiveFromEpochDay`)",
        )

        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `entry` (`id` TEXT NOT NULL, `habitId` TEXT NOT NULL, " +
                "`epochDay` INTEGER NOT NULL, `value` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_entry_habitId_epochDay` " +
                "ON `entry` (`habitId`, `epochDay`)",
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_epochDay` ON `entry` (`epochDay`)")

        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `settings` (`id` INTEGER NOT NULL, `themeMode` TEXT NOT NULL, " +
                "`numeralSystem` TEXT NOT NULL, `latitude` REAL, `longitude` REAL, " +
                "`calculationMethod` TEXT NOT NULL, `privacyMode` TEXT NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )

        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `outbox` (`id` TEXT NOT NULL, `entityType` TEXT NOT NULL, " +
                "`entityId` TEXT NOT NULL, `op` TEXT NOT NULL, `payload` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_outbox_createdAt` ON `outbox` (`createdAt`)")
    }
}

/**
 * Drops `prayer_record`.
 *
 * Habit and entry superseded it, and the last code reading it went when the Today screen
 * moved onto the habit layer. It survived version 2 deliberately, so that no version in
 * between left a database the app could not open.
 *
 * This is the only migration here that destroys data, and it does destroy it: the rows are
 * dropped, not converted into habits and entries. That is acceptable only because the table
 * held nothing but data from the pre-release prototype — no release of the app ever carried
 * it — and the five prayers are a counter habit now. A migration that drops a table a
 * released version wrote would have to convert its rows first.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `prayer_record`")
    }
}

/**
 * Adds a nickname to settings, and renames the privacy modes to the three the product offers.
 *
 * The rename is translated here rather than left to the mapper's unknown-value fallback, so
 * the upgrade says what it means. Anonymous becomes points-only, which is what it always was.
 * Open becomes points-only too rather than the nickname mode: there is no nickname to show,
 * and a translation must never share more than the user already agreed to. Private stays
 * private.
 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `settings` ADD COLUMN `nickname` TEXT")
        connection.execSQL(
            "UPDATE `settings` SET `privacyMode` = 'POINTS_ONLY' " +
                "WHERE `privacyMode` IN ('OPEN', 'ANONYMOUS')",
        )
    }
}

/**
 * Records in settings whether onboarding was finished, starting at not finished.
 *
 * No install could have finished an onboarding that did not exist yet, so nothing is
 * backfilled. An upgraded install with habits is still never sent through onboarding: it is
 * only needed when there is neither the flag nor any habit.
 */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `settings` ADD COLUMN `onboardingCompleted` INTEGER NOT NULL DEFAULT 0")
    }
}

/** Every migration this database has ever had, in order. */
val WIRD_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
