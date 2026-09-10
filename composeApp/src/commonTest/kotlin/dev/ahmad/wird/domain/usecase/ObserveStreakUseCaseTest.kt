package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.model.StreakInfo
import dev.ahmad.wird.domain.usecase.HistoryFixtures.completedOn
import dev.ahmad.wird.domain.usecase.HistoryFixtures.habit
import dev.ahmad.wird.domain.usecase.HistoryFixtures.recentDays
import dev.ahmad.wird.domain.usecase.HistoryFixtures.today
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Wires the stored history into [CalculateStreakUseCase], which is where the streak rules
 * already live and are already pinned. What this adds is the part that can only go wrong
 * against real data: deciding which days count as complete, and over what window.
 */
class ObserveStreakUseCaseTest {

    private fun useCase(habits: FakeHabitRepository, entries: FakeEntryRepository) =
        ObserveStreakUseCase(
            habits = habits,
            entries = entries,
            calculateDayStats = CalculateDayStatsUseCase(),
            calculateStreak = CalculateStreakUseCase(),
            clock = HistoryFixtures.clock,
            zone = HistoryFixtures.zone,
        )

    @Test
    fun reportsNoStreakForAnInstallWithNoHistory() = runTest {
        val streak = useCase(FakeHabitRepository(), FakeEntryRepository())(30).first()

        assertEquals(StreakInfo.NONE, streak)
    }

    @Test
    fun countsAnUnbrokenRunEndingToday() = runTest {
        val days = recentDays(30)
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(completedOn("duha", days.takeLast(4)))

        val streak = useCase(habits, entries)(30).first()

        assertEquals(4, streak.current)
        assertEquals(today, streak.lastCompleteDay)
    }

    @Test
    fun keepsTheRunAliveWhileTodayIsStillUnfinished() = runTest {
        // The headline rule, seen through real data rather than a hand-built set.
        val days = recentDays(30)
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(completedOn("duha", days.dropLast(1).takeLast(3)))

        val streak = useCase(habits, entries)(30).first()

        assertEquals(3, streak.current)
    }

    @Test
    fun doesNotCountADayWhereOnlySomeHabitsWereDone() = runTest {
        // Completeness is the whole routine, not any single habit.
        val days = recentDays(30)
        val habits = FakeHabitRepository(listOf(habit("duha"), habit("witr", sortOrder = 1)))
        val entries = FakeEntryRepository(completedOn("duha", days.takeLast(3)))

        val streak = useCase(habits, entries)(30).first()

        assertEquals(StreakInfo.NONE, streak)
    }

    @Test
    fun countsADayWhereEveryHabitWasDone() = runTest {
        val days = recentDays(30)
        val habits = FakeHabitRepository(listOf(habit("duha"), habit("witr", sortOrder = 1)))
        val entries = FakeEntryRepository(
            completedOn("duha", days.takeLast(2)) + completedOn("witr", days.takeLast(2)),
        )

        val streak = useCase(habits, entries)(30).first()

        assertEquals(2, streak.current)
    }

    @Test
    fun doesNotCountADayThatHadNoHabitsAtAll() = runTest {
        // A day before the routine existed has nothing to complete, so it cannot extend a
        // run across the gap.
        val days = recentDays(10)
        val habits = FakeHabitRepository(listOf(habit("duha", effectiveFrom = days[8])))
        val entries = FakeEntryRepository(completedOn("duha", days.takeLast(2)))

        val streak = useCase(habits, entries)(10).first()

        assertEquals(2, streak.current)
        assertEquals(2, streak.longest)
    }

    @Test
    fun measuresOnlyWithinTheWindowItIsGiven() = runTest {
        // A run reaching back beyond the window is measured from the window's edge. That
        // is a deliberate limit, not an accident, so it is pinned.
        val days = recentDays(30)
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(completedOn("duha", days))

        val narrow = useCase(habits, entries)(5).first()
        val wide = useCase(habits, entries)(30).first()

        assertEquals(5, narrow.current)
        assertEquals(30, wide.current)
    }

    @Test
    fun handlesFourHundredDaysOfHistory() = runTest {
        val days = recentDays(400)
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(completedOn("duha", days))

        val streak = useCase(habits, entries)(400).first()

        assertEquals(400, streak.current)
        assertEquals(400, streak.longest)
    }
}
