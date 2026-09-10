package dev.ahmad.wird.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Reads and updates effective-dated habit revisions. */
@Dao
interface HabitDao {

    // Only open revisions belong in today's active-habit list.
    @Query("SELECT * FROM habit WHERE retiredOnEpochDay IS NULL ORDER BY sortOrder")
    fun observeActive(): Flow<List<HabitEntity>>

    // effectiveFrom is inclusive and retiredOn is exclusive so boundary days select one revision.
    @Query("SELECT * FROM habit WHERE effectiveFromEpochDay <= :epochDay AND (retiredOnEpochDay IS NULL OR retiredOnEpochDay > :epochDay) ORDER BY sortOrder")
    fun observeOn(epochDay: Long): Flow<List<HabitEntity>>

    // effectiveFrom is inclusive and retiredOn is exclusive so either range boundary can overlap.
    @Query("SELECT * FROM habit WHERE effectiveFromEpochDay <= :toEpochDay AND (retiredOnEpochDay IS NULL OR retiredOnEpochDay > :fromEpochDay) ORDER BY sortOrder")
    fun observeIn(fromEpochDay: Long, toEpochDay: Long): Flow<List<HabitEntity>>

    // Seeding depends on whether any revision ever existed, including retired ones.
    @Query("SELECT EXISTS(SELECT 1 FROM habit)")
    suspend fun hasAny(): Boolean

    // Only the open revision can be edited as the current habit.
    @Query("SELECT * FROM habit WHERE habitId = :habitId AND retiredOnEpochDay IS NULL LIMIT 1")
    suspend fun currentRevision(habitId: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity)

    // Closing a habit must preserve every already-retired historical boundary.
    @Query("UPDATE habit SET retiredOnEpochDay = :epochDay WHERE habitId = :habitId AND retiredOnEpochDay IS NULL")
    suspend fun retire(habitId: String, epochDay: Long)

    // Reinstatement reopens the stored revision for this stable habit identity.
    @Query("UPDATE habit SET retiredOnEpochDay = NULL WHERE habitId = :habitId")
    suspend fun reinstate(habitId: String)

    // Presentation order applies consistently to current and historical views.
    @Query("UPDATE habit SET sortOrder = :sortOrder WHERE habitId = :habitId")
    suspend fun setSortOrder(habitId: String, sortOrder: Int)
}
