package dev.ahmad.wird.data.repository

import dev.ahmad.wird.data.local.HabitDao
import dev.ahmad.wird.data.mapper.toDomain
import dev.ahmad.wird.data.mapper.toEntity
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Clock

/**
 * Local-first, and history-preserving: a habit is never edited in place.
 *
 * Revising closes the current revision at the day the edit takes effect and opens a new
 * one from there, so every day before it keeps the target that was actually in force then.
 * That is why there is no `update` here — only [upsert], which means "from this day
 * onward".
 */
class HabitRepositoryImpl(
    private val habits: HabitDao,
    private val outbox: OutboxWriter,
    private val clock: Clock,
    private val newId: () -> String,
) : HabitRepository {

    override fun observeActiveHabits(): Flow<List<Habit>> =
        habits.observeActive()
            .seededWith { habits.getActive() }
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeHabitsOn(day: LocalDate): Flow<List<Habit>> =
        habits.observeOn(day.toEpochDays())
            .seededWith { habits.getOn(day.toEpochDays()) }
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeHabitsIn(from: LocalDate, to: LocalDate): Flow<List<Habit>> =
        habits.observeIn(from.toEpochDays(), to.toEpochDays())
            .seededWith { habits.getIn(from.toEpochDays(), to.toEpochDays()) }
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun hasAnyHabit(): Boolean = habits.hasAny()

    override suspend fun upsert(habit: Habit) {
        // Close whatever is currently open before opening the new revision, so the two
        // never overlap on a day and no day can see the habit twice. Retiring only ever
        // touches an open revision, so this is a no-op when the habit is new.
        habits.retire(habit.id, habit.effectiveFrom.toEpochDays())

        val row = habit.toEntity(revisionId = newId(), updatedAt = clock.now().toEpochMilliseconds())
        habits.insert(row)

        outbox.record(
            entityType = OutboxWriter.TYPE_HABIT,
            // The stable habit id, not the revision id: a server syncs habits, and the
            // revision is a storage detail of how history is kept.
            entityId = habit.id,
            op = OutboxWriter.OP_UPSERT,
            payload = buildJsonObject {
                put("habitId", JsonPrimitive(row.habitId))
                put("name", JsonPrimitive(row.name))
                put("kind", JsonPrimitive(row.kind))
                put("target", JsonPrimitive(row.target))
                put("iconKey", JsonPrimitive(row.iconKey))
                put("sortOrder", JsonPrimitive(row.sortOrder))
                put("effectiveFromEpochDay", JsonPrimitive(row.effectiveFromEpochDay))
                put("updatedAt", JsonPrimitive(row.updatedAt))
            },
        )
    }

    override suspend fun reorder(habitIdsInOrder: List<String>) {
        habitIdsInOrder.forEachIndexed { position, habitId ->
            habits.setSortOrder(habitId, position)
        }

        outbox.record(
            entityType = OutboxWriter.TYPE_HABIT,
            entityId = habitIdsInOrder.joinToString(","),
            op = OutboxWriter.OP_REORDER,
            payload = buildJsonObject {
                put("order", JsonPrimitive(habitIdsInOrder.joinToString(",")))
                put("updatedAt", JsonPrimitive(clock.now().toEpochMilliseconds()))
            },
        )
    }

    override suspend fun setActive(habitId: String, active: Boolean, asOf: LocalDate) {
        if (active) habits.reinstate(habitId) else habits.retire(habitId, asOf.toEpochDays())

        outbox.record(
            entityType = OutboxWriter.TYPE_HABIT,
            entityId = habitId,
            op = OutboxWriter.OP_SET_ACTIVE,
            payload = buildJsonObject {
                put("habitId", JsonPrimitive(habitId))
                put("active", JsonPrimitive(active))
                put("asOfEpochDay", JsonPrimitive(asOf.toEpochDays()))
                put("updatedAt", JsonPrimitive(clock.now().toEpochMilliseconds()))
            },
        )
    }
}
