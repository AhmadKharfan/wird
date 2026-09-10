package dev.ahmad.wird.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/**
 * Version 3 drops `prayer_record`, which habit and entry superseded. Nothing has read it
 * since the Today screen moved onto the habit layer.
 */
@Database(
    entities = [
        HabitEntity::class,
        EntryEntity::class,
        SettingsEntity::class,
        OutboxEntity::class,
    ],
    version = 3,
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
