package dev.ahmad.wird.data.repository

import dev.ahmad.wird.data.local.RoomLocalTransaction
import dev.ahmad.wird.data.local.createTestDatabase
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The export reads everything, against a real database. Every other read in the app is
 * windowed to the days in force; these two are the only ones that must return retired
 * revisions and entries from any day at all.
 */
class ExportReadsTest {

    private val database = createTestDatabase()

    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000)
    }

    private var nextId = 0
    private val ids = { "id-${nextId++}" }

    private val outbox = OutboxWriter(database.outboxDao(), clock, ids)
    private val habits = HabitRepositoryImpl(database.habitDao(), outbox, clock, ids)
    private val entries = EntryRepositoryImpl(database.entryDao(), outbox, RoomLocalTransaction(database), clock, ids)

    @AfterTest
    fun closeDatabase() = database.close()

    private fun habit(id: String, target: Int, kind: HabitKind, from: LocalDate) = Habit(
        id = id,
        name = id,
        kind = kind,
        target = target,
        iconKey = id,
        sortOrder = 0,
        effectiveFrom = from,
    )

    @Test
    fun readsEveryRevisionIncludingRetiredOnes() = runTest {
        habits.upsert(habit("prayers", target = 3, kind = HabitKind.COUNTER, from = LocalDate(2026, 1, 10)))
        habits.upsert(habit("prayers", target = 5, kind = HabitKind.COUNTER, from = LocalDate(2026, 1, 15)))
        habits.upsert(habit("witr", target = 1, kind = HabitKind.BOOL, from = LocalDate(2026, 1, 10)))
        habits.setActive("witr", active = false, asOf = LocalDate(2026, 1, 12))

        val every = habits.allRevisions()

        assertEquals(3, every.size)
        assertEquals(setOf(3, 5, 1), every.map { it.target }.toSet())
        assertEquals(LocalDate(2026, 1, 12), every.single { it.id == "witr" }.retiredOn)
    }

    @Test
    fun readsEveryEntryOnEveryDay() = runTest {
        entries.setValue("prayers", LocalDate(2025, 12, 31), 5)
        entries.setValue("prayers", LocalDate(2026, 1, 14), 3)
        entries.setValue("duha", LocalDate(2026, 1, 15), 1)

        assertEquals(3, entries.allEntries().size)
    }

    @Test
    fun readsNothingFromAnEmptyDatabase() = runTest {
        assertEquals(emptyList(), habits.allRevisions())
        assertEquals(emptyList(), entries.allEntries())
    }
}
