package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.StreakInfo
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A streak is a run of complete days ending now. The rule that shapes this whole class is
 * that **an unfinished today does not break the run** — the day is not over, so the walk
 * starts at yesterday instead of returning zero. Only a second unfinished day ends it.
 */
class CalculateStreakUseCaseTest {

    private val calculate = CalculateStreakUseCase()

    private fun jan(day: Int) = LocalDate(2026, 1, day)

    private val today = jan(15)

    // --- nothing to count ---------------------------------------------------------------

    @Test
    fun reportsNoStreakWhenNoDayWasEverCompleted() {
        assertEquals(StreakInfo.NONE, calculate(today, completeDays = emptySet()))
    }

    @Test
    fun reportsNoCurrentRunWhenTheLastCompleteDayIsLongPast() {
        val streak = calculate(today, completeDays = setOf(jan(5)))

        assertEquals(0, streak.current)
        assertEquals(1, streak.longest)
        assertEquals(jan(5), streak.lastCompleteDay)
    }

    // --- the rule about today -------------------------------------------------------------

    @Test
    fun countsTodayWhenTodayIsComplete() {
        val streak = calculate(today, completeDays = setOf(today))

        assertEquals(1, streak.current)
        assertEquals(1, streak.longest)
        assertEquals(today, streak.lastCompleteDay)
    }

    @Test
    fun keepsTheRunAliveWhileTodayIsStillUnfinished() {
        // The headline rule. It is the 15th, nothing done yet, but the 13th and 14th were
        // both complete. The run is two and stands until the day actually ends.
        val streak = calculate(today, completeDays = setOf(jan(13), jan(14)))

        assertEquals(2, streak.current)
        assertEquals(2, streak.longest)
        assertEquals(jan(14), streak.lastCompleteDay)
    }

    @Test
    fun breaksTheRunOnlyOnceASecondDayIsMissed() {
        // Yesterday was missed and is now over, so the run really has ended, even though
        // today is merely unfinished.
        val streak = calculate(today, completeDays = setOf(jan(7), jan(8), jan(9), jan(10)))

        assertEquals(0, streak.current)
        assertEquals(4, streak.longest)
        assertEquals(jan(10), streak.lastCompleteDay)
    }

    @Test
    fun countsAnUnbrokenRunUpToAndIncludingToday() {
        val streak = calculate(today, completeDays = setOf(jan(12), jan(13), jan(14), jan(15)))

        assertEquals(4, streak.current)
        assertEquals(4, streak.longest)
        assertEquals(today, streak.lastCompleteDay)
    }

    // --- the record ---------------------------------------------------------------------------

    @Test
    fun remembersALongerRunFromEarlier() {
        // Five days at the start of the month, then a gap, then two days ending today.
        val streak = calculate(
            today,
            completeDays = setOf(jan(1), jan(2), jan(3), jan(4), jan(5), jan(14), jan(15)),
        )

        assertEquals(2, streak.current)
        assertEquals(5, streak.longest)
        assertEquals(today, streak.lastCompleteDay)
    }

    @Test
    fun reportsTheCurrentRunAsTheRecordWhenItIsTheLongest() {
        val streak = calculate(
            today,
            completeDays = setOf(jan(1), jan(2), jan(11), jan(12), jan(13), jan(14), jan(15)),
        )

        assertEquals(5, streak.current)
        assertEquals(5, streak.longest)
    }

    @Test
    fun doesNotCareWhatOrderTheDaysArriveIn() {
        val scrambled = setOf(jan(14), jan(2), jan(15), jan(1), jan(13))
        val sorted = setOf(jan(1), jan(2), jan(13), jan(14), jan(15))

        assertEquals(calculate(today, sorted), calculate(today, scrambled))
    }

    // --- calendar boundaries ---------------------------------------------------------------------

    @Test
    fun runsAcrossTheEndOfAMonth() {
        val streak = calculate(
            today = LocalDate(2026, 2, 2),
            completeDays = setOf(
                LocalDate(2026, 1, 30),
                LocalDate(2026, 1, 31),
                LocalDate(2026, 2, 1),
                LocalDate(2026, 2, 2),
            ),
        )

        assertEquals(4, streak.current)
    }

    @Test
    fun runsAcrossTheEndOfAYear() {
        val streak = calculate(
            today = LocalDate(2026, 1, 2),
            completeDays = setOf(
                LocalDate(2025, 12, 30),
                LocalDate(2025, 12, 31),
                LocalDate(2026, 1, 1),
                LocalDate(2026, 1, 2),
            ),
        )

        assertEquals(4, streak.current)
    }

    @Test
    fun runsThroughALeapDay() {
        val streak = calculate(
            today = LocalDate(2024, 3, 1),
            completeDays = setOf(
                LocalDate(2024, 2, 28),
                LocalDate(2024, 2, 29),
                LocalDate(2024, 3, 1),
            ),
        )

        assertEquals(3, streak.current)
    }

    @Test
    fun runsStraightFromFebruaryToMarchInACommonYear() {
        // 2026 has no 29 February. An implementation that assumed one would look for a
        // day that does not exist, find nothing, and break the run here.
        val streak = calculate(
            today = LocalDate(2026, 3, 1),
            completeDays = setOf(LocalDate(2026, 2, 28), LocalDate(2026, 3, 1)),
        )

        assertEquals(2, streak.current)
    }

    // --- days recorded ahead of today ------------------------------------------------------------

    @Test
    fun doesNotCountDaysAheadOfTodayTowardTheCurrentRun() {
        // Flying west moves today backwards, so a day already recorded can sit ahead of
        // it. That day genuinely happened and belongs to the record, but the run the user
        // is currently on is measured from today.
        val streak = calculate(today, completeDays = setOf(jan(14), jan(15), jan(16)))

        assertEquals(2, streak.current)
        assertEquals(3, streak.longest)
        assertEquals(jan(16), streak.lastCompleteDay)
    }
}
