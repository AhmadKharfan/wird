package dev.ahmad.wird.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * One local write, queued for a server that does not exist yet. Never leaves `data/`.
 *
 * Every repository write appends here in the same transaction as the write itself, so the
 * queue can never disagree with the data. **Nothing drains it** — there is no sync layer
 * in this pack. It exists now because retrofitting an outbox after the fact means
 * reopening every write path; building it now means the sync layer is a reader of an
 * already-correct queue.
 *
 * [payload] is the row as JSON, captured at write time. Storing the payload rather than
 * re-reading the row at drain time is what makes the queue an ordered log of intent
 * instead of a list of pointers to whatever the row happens to say later.
 *
 * [attempts] is for the future drainer's backoff; it stays 0 here.
 */
@Entity(
    tableName = "outbox",
    indices = [Index(value = ["createdAt"])],
)
data class OutboxEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val op: String,
    val payload: String,
    val createdAt: Long,
    val attempts: Int = 0,
)
