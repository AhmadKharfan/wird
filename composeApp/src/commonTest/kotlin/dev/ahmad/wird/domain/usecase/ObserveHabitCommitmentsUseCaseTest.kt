package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.model.HabitKind
import dev.ahmad.wird.domain.usecase.HistoryFixtures.completedOn
import dev.ahmad.wird.domain.usecase.HistoryFixtures.habit
import dev.ahmad.wird.domain.usecase.HistoryFixtures.recentDays
import dev.ahmad.wird.domain.usecase.HistoryFixtures.today
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Feeds the per-habit commitment bars.
 *
 * The ordering is the product decision: **weakest first**, so the screen reads as a plan
 * rather than a trophy case. Ties break on the habit's own display order, so a list of
 * equal habits does not shuffle between emissions.
 */
class ObserveHabitCommitmentsUseCaseTest {

    private val days = recentDays(10)
    private val from = days.first()

    private fun useCase(habits: FakeHabitRepository, entries: FakeEntryRepository) =
        ObserveHabitCommitmentsUseCase(habits = habits, entries = entries)

    @Test
    fun ordersTheWeakestHabitFirst() = runTest {
        val habits = FakeHabitRepository(
            listOf(habit("strong", sortOrder = 0), habit("weak", sortOrder = 1)),
        )
        val entries = FakeEntryRepository(
            completedOn("strong", days) + completedOn("weak", days.take(2)),
        )

        val commitments = useCase(habits, entries)(from, today).first()

        assertEquals(listOf("weak", "strong"), commitments.map { it.habit.id })
    }

    @Test
    fun breaksTiesOnDisplayOrderSoTheListDoesNotShuffle() = runTest {
        val habits = FakeHabitRepository(
            listOf(habit("second", sortOrder = 1), habit("first", sortOrder = 0)),
        )

        val commitments = useCase(habits, FakeEntryRepository())(from, today).first()

        assertEquals(listOf("first", "second"), commitments.map { it.habit.id })
    }

    @Test
    fun countsCompletedDaysAgainstLiveDays() = runTest {
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(completedOn("duha", days.take(4)))

        val commitment = useCase(habits, entries)(from, today).first().single()

        assertEquals(10, commitment.elapsedDays)
        assertEquals(4, commitment.completedDays)
        assertEquals(0.4f, commitment.ratio, 1e-6f)
    }

    @Test
    fun judgesAHabitOnlyOverTheDaysItExisted() = runTest {
        // Added halfway through the window, done every day since: that is 100%, not 50%.
        val habits = FakeHabitRepository(listOf(habit("duha", effectiveFrom = days[5])))
        val entries = FakeEntryRepository(completedOn("duha", days.drop(5)))

        val commitment = useCase(habits, entries)(from, today).first().single()

        assertEquals(5, commitment.elapsedDays)
        assertEquals(5, commitment.completedDays)
        assertEquals(1f, commitment.ratio, 1e-6f)
    }

    @Test
    fun countsACounterDayOnlyWhenItReachesItsTarget() = runTest {
        val habits = FakeHabitRepository(listOf(habit("prayers", HabitKind.COUNTER, target = 5)))
        val entries = FakeEntryRepository(
            completedOn("prayers", days.take(3), value = 5) +
                completedOn("prayers", days.drop(3).take(4), value = 4),
        )

        val commitment = useCase(habits, entries)(from, today).first().single()

        assertEquals(3, commitment.completedDays)
    }

    @Test
    fun addsUpBothSidesOfATargetChange() = runTest {
        // One habit, two revisions inside the window. Counting only one of them would
        // silently shorten the habit's history and flatter its ratio.
        val habits = FakeHabitRepository(
            listOf(
                habit("prayers", HabitKind.COUNTER, target = 3, retiredOn = days[5]),
                habit("prayers", HabitKind.COUNTER, target = 5, effectiveFrom = days[5]),
            ),
        )
        val entries = FakeEntryRepository(completedOn("prayers", days, value = 3))

        val commitment = useCase(habits, entries)(from, today).first().single()

        // Ten live days in total; the first five met a target of 3, the rest did not.
        assertEquals(10, commitment.elapsedDays)
        assertEquals(5, commitment.completedDays)
    }

    @Test
    fun reportsTheCurrentRevisionSoTheBarIsLabelledWithTodaysTarget() = runTest {
        val habits = FakeHabitRepository(
            listOf(
                habit("prayers", HabitKind.COUNTER, target = 3, retiredOn = days[5]),
                habit("prayers", HabitKind.COUNTER, target = 5, effectiveFrom = days[5]),
            ),
        )

        val commitment = useCase(habits, FakeEntryRepository())(from, today).first().single()

        assertEquals(5, commitment.habit.target)
    }

    @Test
    fun leavesOutAHabitRetiredBeforeTheWindow() = runTest {
        val habits = FakeHabitRepository(listOf(habit("gone", retiredOn = from)))

        assertEquals(emptyList(), useCase(habits, FakeEntryRepository())(from, today).first())
    }

    @Test
    fun readsAnEmptyRoutineAsAnEmptyListRatherThanFailing() = runTest {
        assertEquals(
            emptyList(),
            useCase(FakeHabitRepository(), FakeEntryRepository())(from, today).first(),
        )
    }

    @Test
    fun handlesFourHundredDaysOfHistory() = runTest {
        val longWindow = recentDays(400)
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(completedOn("duha", longWindow.take(100)))

        val commitment = useCase(habits, entries)(longWindow.first(), today).first().single()

        assertEquals(400, commitment.elapsedDays)
        assertEquals(100, commitment.completedDays)
        assertTrue(commitment.ratio > 0.24f && commitment.ratio < 0.26f)
    }
}
