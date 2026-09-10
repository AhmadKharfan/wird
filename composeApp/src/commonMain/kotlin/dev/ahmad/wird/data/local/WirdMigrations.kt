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
// The statements below are copied verbatim from the generated schema JSON rather than
// written by hand. Room validates the migrated database against that schema on open and
// refuses to start on any difference, down to an index name — so hand-written SQL that
// merely looks equivalent fails at runtime rather than at build time.

/**
 * Adds the habit, entry, settings and outbox tables.
 *
 * `prayer_record` is deliberately left alone. It is superseded by habit/entry, but the code
 * that reads it is still live at this version; it is dropped in the migration that lands
 * with its removal, so no version in between has a database the app cannot open.
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

/** Every migration this database has ever had, in order. */
val WIRD_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
