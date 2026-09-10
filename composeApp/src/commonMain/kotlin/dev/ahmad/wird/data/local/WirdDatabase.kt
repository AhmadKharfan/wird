package dev.ahmad.wird.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [PrayerRecordEntity::class], version = 1)
@ConstructedBy(WirdDatabaseConstructor::class)
abstract class WirdDatabase : RoomDatabase() {
    abstract fun prayerRecordDao(): PrayerRecordDao
}

/** Room generates the `actual` for each target; do not hand-write one. */
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object WirdDatabaseConstructor : RoomDatabaseConstructor<WirdDatabase> {
    override fun initialize(): WirdDatabase
}
