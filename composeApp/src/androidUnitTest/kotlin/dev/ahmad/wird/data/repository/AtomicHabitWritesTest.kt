package dev.ahmad.wird.data.repository

import dev.ahmad.wird.data.local.OutboxDao
import dev.ahmad.wird.data.local.OutboxEntity
import dev.ahmad.wird.data.local.RoomLocalTransaction
import dev.ahmad.wird.data.local.createTestDatabase
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Habit writes are several statements each — an edit closes one revision and opens the
 * next — so a failure part-way through must undo all of them, outbox record included.
 */
class AtomicHabitWritesTest {

    private val database = createTestDatabase()
    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000)
    }
    private var nextId = 0
    private val ids = { "id-${nextId++}" }
    private val transaction = RoomLocalTransaction(database)

    private val working = HabitRepositoryImpl(
        database.habitDao(),
        OutboxWriter(database.outboxDao(), clock, ids),
        transaction,
        clock,
        ids,
    )
    private val failing = HabitRepositoryImpl(
        database.habitDao(),
        OutboxWriter(
            dao = object : OutboxDao by database.outboxDao() {
                override suspend fun append(record: OutboxEntity) {
                    throw IllegalStateException("disk full")
                }
            },
            clock = clock,
            newId = ids,
        ),
        transaction,
        clock,
        ids,
    )

    private fun habit(id: String, target: Int = 1, from: LocalDate = LocalDate(2026, 1, 10)) = Habit(
        id = id,
        name = id,
        kind = if (target == 1) HabitKind.BOOL else HabitKind.COUNTER,
        target = target,
        iconKey = id,
        sortOrder = 0,
        effectiveFrom = from,
    )

    @AfterTest
    fun closeDatabase() = database.close()

    @Test
    fun keepsTheCurrentRevisionInForceWhenAnEditCannotBeQueued() = runTest {
        // Half an edit is a closed revision with nothing opened after it, or an opened one
        // the sync layer never hears about.
        working.upsert(habit("prayers", target = 3))

        assertFailsWith<IllegalStateException> { failing.upsert(habit("prayers", target = 5, from = LocalDate(2026, 1, 15))) }

        assertEquals(listOf(3), database.habitDao().getActive().map { it.target })
    }

    @Test
    fun plantsNoPartOfARoutineThatCannotBeStoredInFull() = runTest {
        // A seed that stops half way would count as "a habit has existed" from then on, and
        // the rest of the routine would never be planted.
        assertFailsWith<IllegalStateException> { failing.upsertAll(listOf(habit("a"), habit("b"))) }

        assertFalse(database.habitDao().hasAny())
    }
}
