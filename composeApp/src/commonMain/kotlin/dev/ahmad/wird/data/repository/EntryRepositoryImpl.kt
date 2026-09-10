package dev.ahmad.wird.data.repository

import dev.ahmad.wird.data.local.EntryDao
import dev.ahmad.wird.data.local.EntryEntity
import dev.ahmad.wird.data.local.LocalTransaction
import dev.ahmad.wird.data.mapper.toDomain
import dev.ahmad.wird.domain.model.Entry
import dev.ahmad.wird.domain.repository.EntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Clock

/**
 * Local-first: every write lands in the local database and returns. Nothing here awaits a
 * network, and the outbox record is appended in the same transaction as the write, so the
 * queue can never disagree with the data.
 */
class EntryRepositoryImpl(
    private val entries: EntryDao,
    private val outbox: OutboxWriter,
    private val transaction: LocalTransaction,
    private val clock: Clock,
    private val newId: () -> String,
) : EntryRepository {

    override fun observeDay(day: LocalDate): Flow<List<Entry>> =
        entries.observeDay(day.toEpochDays())
            .seededWith { entries.getDay(day.toEpochDays()) }
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Entry>> =
        entries.observeRange(from.toEpochDays(), to.toEpochDays())
            .seededWith { entries.getRange(from.toEpochDays(), to.toEpochDays()) }
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun setValue(habitId: String, day: LocalDate, value: Int) {
        transaction.write { write(habitId, day, value) }
    }

    override suspend fun allEntries(): List<Entry> = entries.getAll().map { it.toDomain() }

    override suspend fun toggle(habitId: String, day: LocalDate) {
        // The read belongs inside the transaction too, so two quick taps cannot both flip the
        // same old value.
        transaction.write {
            val current = entries.find(habitId, day.toEpochDays())?.value ?: 0
            write(habitId, day, if (current == 0) 1 else 0)
        }
    }

    /**
     * Reuses the stored row's id when one exists.
     *
     * This is the whole reason the primary key is client-generated. Replacing through the
     * unique `(habitId, epochDay)` index is a delete-then-insert, so a fresh id on every
     * write would rename the row each time it changed and a sync layer would see one row
     * as many.
     */
    private suspend fun write(habitId: String, day: LocalDate, value: Int) {
        val existing = entries.find(habitId, day.toEpochDays())
        val row = EntryEntity(
            id = existing?.id ?: newId(),
            habitId = habitId,
            epochDay = day.toEpochDays(),
            value = value,
            updatedAt = clock.now().toEpochMilliseconds(),
        )

        entries.insert(row)
        outbox.record(
            entityType = OutboxWriter.TYPE_ENTRY,
            entityId = row.id,
            op = OutboxWriter.OP_UPSERT,
            payload = buildJsonObject {
                put("id", JsonPrimitive(row.id))
                put("habitId", JsonPrimitive(row.habitId))
                put("epochDay", JsonPrimitive(row.epochDay))
                put("value", JsonPrimitive(row.value))
                put("updatedAt", JsonPrimitive(row.updatedAt))
            },
        )
    }
}
