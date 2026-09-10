package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.usecase.HistoryFixtures.completedOn
import dev.ahmad.wird.domain.usecase.HistoryFixtures.habit
import dev.ahmad.wird.domain.usecase.HistoryFixtures.recentDays
import dev.ahmad.wird.domain.usecase.HistoryFixtures.today
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Feeds the bar chart. The window always ends on today and is ordered **oldest first**, so
 * the chart's own time axis is the only place a direction decision is made — the data does
 * not quietly carry a second one.
 */
class ObserveRecentDaysUseCaseTest {

    private fun useCase(habits: FakeHabitRepository, entries: FakeEntryRepository) =
        ObserveRecentDaysUseCase(
            habits = habits,
            entries = entries,
            calculateDayStats = CalculateDayStatsUseCase(),
            clock = HistoryFixtures.clock,
            zone = HistoryFixtures.zone,
        )

    @Test
    fun returnsExactlyTheNumberOfDaysAskedFor() = runTest {
        val scores = useCase(FakeHabitRepository(listOf(habit("duha"))), FakeEntryRepository())(7)
            .first()

        assertEquals(7, scores.size)
    }

    @Test
    fun endsOnTodayAndStartsTheRequestedNumberOfDaysBack() = runTest {
        val scores = useCase(FakeHabitRepository(listOf(habit("duha"))), FakeEntryRepository())(7)
            .first()

        assertEquals(today.plusDays(-6), scores.first().day)
        assertEquals(today, scores.last().day)
    }

    @Test
    fun ordersDaysOldestFirst() = runTest {
        val scores = useCase(FakeHabitRepository(listOf(habit("duha"))), FakeEntryRepository())(7)
            .first()

        assertEquals(scores.map { it.day }.sorted(), scores.map { it.day })
    }

    @Test
    fun returnsJustTodayForAWindowOfOne() = runTest {
        val scores = useCase(FakeHabitRepository(listOf(habit("duha"))), FakeEntryRepository())(1)
            .first()

        assertEquals(listOf(today), scores.map { it.day })
    }

    @Test
    fun rejectsAnEmptyWindow() = runTest {
        assertFailsWith<IllegalArgumentException> {
            useCase(FakeHabitRepository(), FakeEntryRepository())(0).first()
        }
    }

    @Test
    fun scoresEachDayFromItsOwnEntries() = runTest {
        val days = recentDays(7)
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(completedOn("duha", listOf(days[1], days[4])))

        val scores = useCase(habits, entries)(7).first()

        assertEquals(listOf(days[1], days[4]), scores.filter { it.stats.isComplete }.map { it.day })
    }

    @Test
    fun scoresADayWithNoHabitsAsEmptyRatherThanFailing() = runTest {
        val scores = useCase(FakeHabitRepository(), FakeEntryRepository())(7).first()

        assertEquals(7, scores.size)
        assertTrue(scores.all { it.stats.maxPoints == 0 })
        assertTrue(scores.none { it.stats.isComplete })
    }

    @Test
    fun leavesOutAHabitFromDaysBeforeItExisted() = runTest {
        // A habit added three days ago must not make the four days before it look missed.
        val days = recentDays(7)
        val habits = FakeHabitRepository(listOf(habit("duha", effectiveFrom = days[4])))

        val scores = useCase(habits, FakeEntryRepository())(7).first()

        assertEquals(listOf(0, 0, 0, 0, 1, 1, 1), scores.map { it.stats.maxPoints })
    }

    // --- the three history sizes the charts have to survive -------------------------------

    @Test
    fun handlesThreeDaysOfHistory() = runTest {
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(completedOn("duha", recentDays(3)))

        val scores = useCase(habits, entries)(3).first()

        assertEquals(3, scores.size)
        assertTrue(scores.all { it.stats.isComplete })
    }

    @Test
    fun handlesFortyDaysOfHistory() = runTest {
        val days = recentDays(40)
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(completedOn("duha", days.filterIndexed { i, _ -> i % 2 == 0 }))

        val scores = useCase(habits, entries)(40).first()

        assertEquals(40, scores.size)
        assertEquals(20, scores.count { it.stats.isComplete })
    }

    @Test
    fun handlesFourHundredDaysOfHistoryAcrossAYearBoundary() = runTest {
        // 400 days back from 15 January 2026 lands in December 2024, so this crosses two
        // year ends and a leap-free February.
        val days = recentDays(400)
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(completedOn("duha", days))

        val scores = useCase(habits, entries)(400).first()

        assertEquals(400, scores.size)
        assertEquals(days.first(), scores.first().day)
        assertEquals(today, scores.last().day)
        assertEquals(400, scores.count { it.stats.isComplete })
    }
}
