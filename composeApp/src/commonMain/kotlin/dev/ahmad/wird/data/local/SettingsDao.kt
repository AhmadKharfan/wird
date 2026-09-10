package dev.ahmad.wird.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Owns the single persisted settings row. */
@Dao
interface SettingsDao {

    // Pinning reads to id 0 preserves the schema-enforced singleton contract.
    @Query("SELECT * FROM settings WHERE id = 0")
    fun observe(): Flow<SettingsEntity?>

    // A missing singleton is meaningful on a fresh install, so the result stays nullable.
    @Query("SELECT * FROM settings WHERE id = 0")
    suspend fun get(): SettingsEntity?

    @Query("SELECT COUNT(*) FROM settings")
    suspend fun countRows(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)
}
