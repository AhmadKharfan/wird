package dev.ahmad.wird.data.repository

import dev.ahmad.wird.data.local.createTestDatabase
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A habit's name and icon are presentational, like its order: renaming "جزء قرآن" should
 * rename it on every day it appears, not only from today onward. That is the opposite of a
 * target change, which must never reach back — so the two are separate operations, and
 * this pins that a rename touches every revision while touching nothing that scores.
 */
class HabitAppearanceTest {

    private val database = createTestDatabase()

    private val jan10 = LocalDate(2026, 1, 10)
    private val jan12 = LocalDate(2026, 1, 12)
    private val jan15 = LocalDate(2026, 1, 15)

    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000)
    }

    private var nextId = 0
    private val ids = { "id-${nextId++}" }

    private val repository =
        HabitRepositoryImpl(database.habitDao(), OutboxWriter(database.outboxDao(), clock, ids), clock, ids)

    @AfterTest
    fun closeDatabase() = database.close()

    private fun prayers(target: Int, effectiveFrom: LocalDate) = Habit(
        id = "prayers",
        name = "الصلوات",
        kind = HabitKind.COUNTER,
        target = target,
        iconKey = "mosque",
        sortOrder = 0,
        effectiveFrom = effectiveFrom,
    )

    /** Two revisions: a target of 3 from the 10th, then 5 from the 15th. */
    private suspend fun givenARevisedHabit() {
        repository.upsert(prayers(target = 3, effectiveFrom = jan10))
        repository.upsert(prayers(target = 5, effectiveFrom = jan15))
    }

    @Test
    fun renamesEveryRevisionIncludingTheOnesInThePast() = runTest {
        givenARevisedHabit()

        repository.updateAppearance("prayers", name = "الصلوات الخمس", iconKey = "mosque")

        assertEquals("الصلوات الخمس", repository.observeHabitsOn(jan12).first().single().name)
        assertEquals("الصلوات الخمس", repository.observeHabitsOn(jan15).first().single().name)
    }

    @Test
    fun changesTheIconOnEveryRevision() = runTest {
        givenARevisedHabit()

        repository.updateAppearance("prayers", name = "الصلوات", iconKey = "crescent")

        val everyRevision = repository.observeHabitsIn(jan10, jan15).first()
        assertEquals(setOf("crescent"), everyRevision.map { it.iconKey }.toSet())
    }

    @Test
    fun leavesEveryTargetAndWindowExactlyAsItWas() = runTest {
        // The line between this and a target change. A rename that opened a new revision,
        // or rewrote a target, would rescore history.
        givenARevisedHabit()

        repository.updateAppearance("prayers", name = "الصلوات الخمس", iconKey = "crescent")

        assertEquals(3, repository.observeHabitsOn(jan12).first().single().target)
        assertEquals(5, repository.observeHabitsOn(jan15).first().single().target)
        assertEquals(2, repository.observeHabitsIn(jan10, jan15).first().size)
    }

    @Test
    fun leavesOtherHabitsAlone() = runTest {
        givenARevisedHabit()
        repository.upsert(prayers(target = 1, effectiveFrom = jan10).copy(id = "duha", name = "الضحى", kind = HabitKind.BOOL))

        repository.updateAppearance("prayers", name = "الصلوات الخمس", iconKey = "mosque")

        assertEquals(
            "الضحى",
            repository.observeHabitsOn(jan15).first().single { it.id == "duha" }.name,
        )
    }

    @Test
    fun rejectsABlankNameAndChangesNothing() = runTest {
        // A blank name would make every later read of this habit fail Habit's own check,
        // so it must be stopped before it reaches storage rather than discovered after.
        givenARevisedHabit()

        assertFailsWith<IllegalArgumentException> {
            repository.updateAppearance("prayers", name = "  ", iconKey = "mosque")
        }

        assertEquals("الصلوات", repository.observeHabitsOn(jan15).first().single().name)
    }

    @Test
    fun queuesTheChangeUnderTheStableHabitId() = runTest {
        givenARevisedHabit()
        val before = database.outboxDao().count()

        repository.updateAppearance("prayers", name = "الصلوات الخمس", iconKey = "mosque")

        val queued = database.outboxDao().peek(100).last()
        assertEquals(before + 1, database.outboxDao().count())
        assertEquals("habit", queued.entityType)
        assertEquals("prayers", queued.entityId)
        assertEquals("appearance", queued.op)
    }
}
