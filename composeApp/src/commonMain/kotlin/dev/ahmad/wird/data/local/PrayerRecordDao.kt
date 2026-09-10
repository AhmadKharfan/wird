package dev.ahmad.wird.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerRecordDao {

    @Query("SELECT * FROM prayer_record WHERE epochDay = :epochDay ORDER BY prayer")
    fun observeByDay(epochDay: Long): Flow<List<PrayerRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PrayerRecordEntity)
}
