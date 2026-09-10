package dev.ahmad.wird.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Stores and observes the values recorded for habits on local calendar days. */
@Dao
interface EntryDao {

    @Query("SELECT * FROM entry WHERE epochDay = :epochDay")
    fun observeDay(epochDay: Long): Flow<List<EntryEntity>>

    // Both calendar-day bounds belong in the requested reporting window.
    @Query("SELECT * FROM entry WHERE epochDay >= :fromEpochDay AND epochDay <= :toEpochDay")
    fun observeRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<EntryEntity>>

    // Suspend twins of the two observers above, used to seed each observation with a real
    // read so its first emission never depends on an invalidation.
    @Query("SELECT * FROM entry WHERE epochDay = :epochDay")
    suspend fun getDay(epochDay: Long): List<EntryEntity>

    // Unwindowed on purpose: only the export reads every entry on every day.
    @Query("SELECT * FROM entry")
    suspend fun getAll(): List<EntryEntity>

    @Query("SELECT * FROM entry WHERE epochDay >= :fromEpochDay AND epochDay <= :toEpochDay")
    suspend fun getRange(fromEpochDay: Long, toEpochDay: Long): List<EntryEntity>

    // The natural key lets callers retain the row's stable sync identity.
    @Query("SELECT * FROM entry WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun find(habitId: String, epochDay: Long): EntryEntity?

    // REPLACE relies on the unique natural-key index to replace rather than duplicate a re-record.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EntryEntity)
}
