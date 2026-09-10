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

    // Suspend twins of the three observers above. Every observation is seeded with one of
    // these so its first emission comes from a real read rather than from an invalidation
    // arriving -- which on wasmJs it does not.
    @Query("SELECT * FROM habit WHERE retiredOnEpochDay IS NULL ORDER BY sortOrder")
    suspend fun getActive(): List<HabitEntity>

    @Query("SELECT * FROM habit WHERE effectiveFromEpochDay <= :epochDay AND (retiredOnEpochDay IS NULL OR retiredOnEpochDay > :epochDay) ORDER BY sortOrder")
    suspend fun getOn(epochDay: Long): List<HabitEntity>

    @Query("SELECT * FROM habit WHERE effectiveFromEpochDay <= :toEpochDay AND (retiredOnEpochDay IS NULL OR retiredOnEpochDay > :fromEpochDay) ORDER BY sortOrder")
    suspend fun getIn(fromEpochDay: Long, toEpochDay: Long): List<HabitEntity>

    // Seeding depends on whether any revision ever existed, including retired ones.
    @Query("SELECT EXISTS(SELECT 1 FROM habit)")
    suspend fun hasAny(): Boolean

    // Unwindowed on purpose: only the export reads every revision there has ever been.
    @Query("SELECT * FROM habit")
    suspend fun getAll(): List<HabitEntity>

    // Only the open revision can be edited as the current habit.
    @Query("SELECT * FROM habit WHERE habitId = :habitId AND retiredOnEpochDay IS NULL LIMIT 1")
    suspend fun currentRevision(habitId: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity)

    // Closing a habit must preserve every already-retired historical boundary.
    @Query("UPDATE habit SET retiredOnEpochDay = :epochDay WHERE habitId = :habitId AND retiredOnEpochDay IS NULL")
    suspend fun retire(habitId: String, epochDay: Long)

    // The newest revision, retired or not: the one a reinstatement carries forward.
    @Query("SELECT * FROM habit WHERE habitId = :habitId ORDER BY effectiveFromEpochDay DESC LIMIT 1")
    suspend fun latestRevision(habitId: String): HabitEntity?

    // Reopens exactly one revision; every other retirement boundary is history and stays.
    @Query("UPDATE habit SET retiredOnEpochDay = NULL WHERE revisionId = :revisionId")
    suspend fun reopen(revisionId: String)

    // Presentation order applies consistently to current and historical views.
    @Query("UPDATE habit SET sortOrder = :sortOrder WHERE habitId = :habitId")
    suspend fun setSortOrder(habitId: String, sortOrder: Int)

    // Name and icon are presentational, so like sortOrder they apply to every revision.
    @Query("UPDATE habit SET name = :name, iconKey = :iconKey WHERE habitId = :habitId")
    suspend fun setAppearance(habitId: String, name: String, iconKey: String)
}
