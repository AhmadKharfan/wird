package dev.ahmad.wird.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

/** Appends and reads the ordered queue of writes awaiting synchronization. */
@Dao
interface OutboxDao {

    @Insert
    suspend fun append(record: OutboxEntity)

    // Replay must preserve creation order while bounding each drain batch.
    @Query("SELECT * FROM outbox ORDER BY createdAt ASC LIMIT :limit")
    suspend fun peek(limit: Int): List<OutboxEntity>

    @Query("SELECT COUNT(*) FROM outbox")
    suspend fun count(): Int
}
