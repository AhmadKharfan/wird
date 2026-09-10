package dev.ahmad.wird.data.repository

import dev.ahmad.wird.data.local.OutboxDao
import dev.ahmad.wird.data.local.OutboxEntity
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock

/**
 * Appends one record of intent to the outbox for every local write.
 *
 * **Nothing drains this yet.** It is built now because retrofitting an outbox means
 * reopening every write path that already exists; building it now makes the eventual sync
 * layer a reader of an already-correct queue.
 *
 * The payload is serialised at write time rather than re-read when the queue drains. That
 * is what makes the outbox an ordered log of what actually happened, instead of a list of
 * pointers to whatever the rows happen to say later.
 */
class OutboxWriter(
    private val dao: OutboxDao,
    private val clock: Clock,
    private val newId: () -> String,
) {
    suspend fun record(entityType: String, entityId: String, op: String, payload: JsonObject) {
        dao.append(
            OutboxEntity(
                id = newId(),
                entityType = entityType,
                entityId = entityId,
                op = op,
                payload = payload.toString(),
                createdAt = clock.now().toEpochMilliseconds(),
                attempts = 0,
            ),
        )
    }

    companion object {
        const val TYPE_HABIT = "habit"
        const val TYPE_ENTRY = "entry"
        const val TYPE_SETTINGS = "settings"

        const val OP_UPSERT = "upsert"
        const val OP_REORDER = "reorder"
        const val OP_SET_ACTIVE = "setActive"
        const val OP_APPEARANCE = "appearance"
    }
}
