package dev.ahmad.wird.data.repository

import dev.ahmad.wird.data.local.RoomLocalTransaction
import dev.ahmad.wird.data.local.createTestDatabase
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Editing a habit never rewrites a row in place. `upsert` closes the current revision and
 * opens a new one from the day the edit takes effect, which is what leaves past days
 * scoring against the target that was actually in force then.
 */
class HabitRepositoryImplTest {

    private val database = createTestDatabase()

    private val jan10 = LocalDate(2026, 1, 10)
    private val jan15 = LocalDate(2026, 1, 15)
    private val jan20 = LocalDate(2026, 1, 20)

    private var now = Instant.fromEpochMilliseconds(1_000)
    private val clock = object : Clock {
        override fun now(): Instant = now
    }

    private var nextId = 0
    private val ids = { "id-${nextId++}" }

    private val outbox = OutboxWriter(database.outboxDao(), clock, ids)
    private val repository = HabitRepositoryImpl(database.habitDao(), outbox, RoomLocalTransaction(database), clock, ids)

    @AfterTest
    fun closeDatabase() = database.close()

    private fun habit(
        id: String = "prayers",
        kind: HabitKind = HabitKind.COUNTER,
        target: Int = 5,
        sortOrder: Int = 0,
        effectiveFrom: LocalDate = jan10,
        name: String = id,
    ) = Habit(
        id = id,
        name = name,
        kind = kind,
        target = target,
        iconKey = id,
        sortOrder = sortOrder,
        effectiveFrom = effectiveFrom,
    )

    // --- creating ---------------------------------------------------------------------

    @Test
    fun storesANewHabitAsItsFirstRevision() = runTest {
        repository.upsert(habit())

        assertEquals(listOf("prayers"), repository.observeActiveHabits().first().map { it.id })
    }

    @Test
    fun reportsNoHabitsOnAFreshInstall() = runTest {
        assertFalse(repository.hasAnyHabit())
    }

    @Test
    fun reportsAHabitOnceOneExists() = runTest {
        repository.upsert(habit())

        assertTrue(repository.hasAnyHabit())
    }

    // --- revising ------------------------------------------------------------------------

    @Test
    fun leavesAPastDayScoringAgainstTheOldTarget() = runTest {
        // The rule the whole revision mechanism exists for.
        repository.upsert(habit(target = 3, effectiveFrom = jan10))

        repository.upsert(habit(target = 5, effectiveFrom = jan15))

        assertEquals(listOf(3), repository.observeHabitsOn(LocalDate(2026, 1, 12)).first().map { it.target })
        assertEquals(listOf(5), repository.observeHabitsOn(jan15).first().map { it.target })
    }

    @Test
    fun showsExactlyOneRevisionOnAnyGivenDay() = runTest {
        repository.upsert(habit(target = 3, effectiveFrom = jan10))
        repository.upsert(habit(target = 5, effectiveFrom = jan15))

        assertEquals(1, repository.observeHabitsOn(jan15).first().size)
        assertEquals(1, repository.observeActiveHabits().first().size)
    }

    @Test
    fun returnsBothRevisionsWhenAskedForARangeSpanningTheEdit() = runTest {
        repository.upsert(habit(target = 3, effectiveFrom = jan10))
        repository.upsert(habit(target = 5, effectiveFrom = jan15))

        val spanning = repository.observeHabitsIn(jan10, LocalDate(2026, 1, 20)).first()

        assertEquals(listOf(3, 5), spanning.map { it.target }.sorted())
    }

    // --- deactivating -----------------------------------------------------------------------

    @Test
    fun dropsADeactivatedHabitFromTheDayItIsRetired() = runTest {
        repository.upsert(habit(effectiveFrom = jan10))

        repository.setActive("prayers", active = false, asOf = jan15)

        assertEquals(emptyList(), repository.observeHabitsOn(jan15).first())
        assertEquals(emptyList(), repository.observeActiveHabits().first())
    }

    @Test
    fun keepsADeactivatedHabitOnEveryEarlierDay() = runTest {
        repository.upsert(habit(effectiveFrom = jan10))

        repository.setActive("prayers", active = false, asOf = jan15)

        assertEquals(listOf("prayers"), repository.observeHabitsOn(LocalDate(2026, 1, 14)).first().map { it.id })
    }

    @Test
    fun bringsARetiredHabitBackWhenReinstated() = runTest {
        repository.upsert(habit(effectiveFrom = jan10))
        repository.setActive("prayers", active = false, asOf = jan15)

        repository.setActive("prayers", active = true, asOf = jan15)

        assertEquals(listOf("prayers"), repository.observeActiveHabits().first().map { it.id })
    }

    @Test
    fun leavesTheDaysItWasOffUnscoredWhenReinstated() = runTest {
        // Reinstating brings the habit back from the day it returns. The days it was switched
        // off stay off, or bringing a habit back would rescore every one of them.
        repository.upsert(habit(effectiveFrom = jan10))
        repository.setActive("prayers", active = false, asOf = jan15)

        repository.setActive("prayers", active = true, asOf = jan20)

        assertEquals(emptyList(), repository.observeHabitsOn(LocalDate(2026, 1, 17)).first())
        assertEquals(listOf("prayers"), repository.observeHabitsOn(jan20).first().map { it.id })
    }

    @Test
    fun reinstatesOnlyTheLatestRevisionOfARevisedHabit() = runTest {
        // Reopening every revision would leave two live on the same day, which no day can score.
        repository.upsert(habit(target = 3, effectiveFrom = jan10))
        repository.upsert(habit(target = 5, effectiveFrom = LocalDate(2026, 1, 12)))
        repository.setActive("prayers", active = false, asOf = jan15)

        repository.setActive("prayers", active = true, asOf = jan20)

        assertEquals(listOf(3), repository.observeHabitsOn(LocalDate(2026, 1, 11)).first().map { it.target })
        assertEquals(listOf(5), repository.observeHabitsOn(jan20).first().map { it.target })
        assertEquals(listOf(5), repository.observeActiveHabits().first().map { it.target })
    }

    @Test
    fun leavesAHabitThatIsAlreadyActiveAsItIs() = runTest {
        repository.upsert(habit(effectiveFrom = jan10))

        repository.setActive("prayers", active = true, asOf = jan15)

        assertEquals(listOf(jan10), repository.allRevisions().map { it.effectiveFrom })
    }

    @Test
    fun stillReportsARetiredHabitAsHavingExisted() = runTest {
        // Seeding asks this. A user who retired everything must not have the default
        // routine planted back on them.
        repository.upsert(habit(effectiveFrom = jan10))
        repository.setActive("prayers", active = false, asOf = jan15)

        assertTrue(repository.hasAnyHabit())
    }

    // --- ordering ---------------------------------------------------------------------------

    @Test
    fun reordersHabitsIntoTheGivenSequence() = runTest {
        repository.upsert(habit(id = "a", sortOrder = 0))
        repository.upsert(habit(id = "b", sortOrder = 1))
        repository.upsert(habit(id = "c", sortOrder = 2))

        repository.reorder(listOf("c", "a", "b"))

        assertEquals(listOf("c", "a", "b"), repository.observeActiveHabits().first().map { it.id })
    }

    // --- reading ------------------------------------------------------------------------------

    @Test
    fun emitsStoredHabitsOnAFirstCollectionWithNoWriteToProvokeIt() = runTest {
        // The wasmJs initial-emission defect: without a seeded read, reopening the app
        // showed an empty routine until something was tapped.
        repository.upsert(habit())

        val freshRepository = HabitRepositoryImpl(database.habitDao(), outbox, RoomLocalTransaction(database), clock, ids)

        assertEquals(listOf("prayers"), freshRepository.observeActiveHabits().first().map { it.id })
    }

    @Test
    fun readsAnEmptyRoutineAsEmptyRatherThanFailing() = runTest {
        assertEquals(emptyList(), repository.observeActiveHabits().first())
        assertEquals(emptyList(), repository.observeHabitsOn(jan15).first())
        assertEquals(emptyList(), repository.observeHabitsIn(jan10, jan15).first())
    }

    // --- the outbox -----------------------------------------------------------------------------

    @Test
    fun queuesEveryHabitWrite() = runTest {
        repository.upsert(habit())
        repository.setActive("prayers", active = false, asOf = jan15)
        repository.reorder(listOf("prayers"))

        assertEquals(3, database.outboxDao().count())
    }

    @Test
    fun queuesTheStableHabitIdRatherThanTheRevisionId() = runTest {
        // A server syncs habits, not revisions; the revision id is a storage detail.
        repository.upsert(habit())

        val queued = database.outboxDao().peek(1).single()

        assertEquals("habit", queued.entityType)
        assertEquals("prayers", queued.entityId)
    }
}
