package dev.ahmad.wird.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/**
 * Version 2 adds the habit, entry, settings and outbox tables.
 *
 * `prayer_record` is still declared here on purpose. It is superseded by habit/entry and
 * will be dropped in version 3, when its entity and the code reading it go together — so
 * that every commit in between leaves a database the app can actually open.
 */
@Database(
    entities = [
        PrayerRecordEntity::class,
        HabitEntity::class,
        EntryEntity::class,
        SettingsEntity::class,
        OutboxEntity::class,
    ],
    version = 2,
)
@ConstructedBy(WirdDatabaseConstructor::class)
abstract class WirdDatabase : RoomDatabase() {
    abstract fun prayerRecordDao(): PrayerRecordDao
    abstract fun habitDao(): HabitDao
    abstract fun entryDao(): EntryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun outboxDao(): OutboxDao
}

/** Room generates the `actual` for each target; do not hand-write one. */
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object WirdDatabaseConstructor : RoomDatabaseConstructor<WirdDatabase> {
    override fun initialize(): WirdDatabase
}
