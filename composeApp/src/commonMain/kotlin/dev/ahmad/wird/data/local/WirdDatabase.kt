package dev.ahmad.wird.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/**
 * Version 4 gives settings a nickname and renames the privacy modes. Version 3 dropped
 * `prayer_record`, which habit and entry superseded.
 */
@Database(
    entities = [
        HabitEntity::class,
        EntryEntity::class,
        SettingsEntity::class,
        OutboxEntity::class,
    ],
    version = 4,
)
@ConstructedBy(WirdDatabaseConstructor::class)
abstract class WirdDatabase : RoomDatabase() {
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
