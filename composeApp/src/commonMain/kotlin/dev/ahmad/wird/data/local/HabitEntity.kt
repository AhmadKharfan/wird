package dev.ahmad.wird.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Storage shape for one **revision** of a habit. Never leaves `data/`.
 *
 * A habit is effective-dated, so `habitId` is not unique — editing a target closes the
 * current revision and opens another under the same `habitId`. The primary key is
 * therefore a surrogate [revisionId], and the unique index on `(habitId,
 * effectiveFromEpochDay)` is what stops two revisions of one habit from starting on the
 * same day, which would leave the domain unable to say which target applied.
 *
 * [updatedAt] is epoch milliseconds and is stamped on every write, so the future sync
 * layer has a last-write-wins key without a schema change.
 */
@Entity(
    tableName = "habit",
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["habitId", "effectiveFromEpochDay"], unique = true),
    ],
)
data class HabitEntity(
    @PrimaryKey val revisionId: String,
    val habitId: String,
    val name: String,
    val kind: String,
    val target: Int,
    val iconKey: String,
    val sortOrder: Int,
    val effectiveFromEpochDay: Long,
    val retiredOnEpochDay: Long?,
    val updatedAt: Long,
)
