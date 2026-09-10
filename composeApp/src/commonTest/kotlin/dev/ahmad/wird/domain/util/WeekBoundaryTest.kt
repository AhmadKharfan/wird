package dev.ahmad.wird.domain.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Weeks start on Saturday.
 *
 * The dates below are hand-checked: 1 January 2026 is a Thursday, which makes
 * Saturday 10 January the start of the week that contains Thursday the 15th, and
 * Friday the 16th its last day.
 */
class WeekBoundaryTest {

    private val saturday = LocalDate(2026, 1, 10)
    private val sunday = LocalDate(2026, 1, 11)
    private val monday = LocalDate(2026, 1, 12)
    private val tuesday = LocalDate(2026, 1, 13)
    private val wednesday = LocalDate(2026, 1, 14)
    private val thursday = LocalDate(2026, 1, 15)
    private val friday = LocalDate(2026, 1, 16)
    private val nextSaturday = LocalDate(2026, 1, 17)

    // --- every day of one week maps to the same Saturday --------------------------------

    @Test
    fun startsTheWeekOnSaturdayItself() {
        // A Saturday is its own start; anything else here would shift every week by one.
        assertEquals(saturday, WeekBoundary.startOfWeek(saturday))
    }

    @Test
    fun mapsSundayBackToTheSaturdayBefore() {
        // The case a Monday-start or Sunday-start calendar gets wrong.
        assertEquals(saturday, WeekBoundary.startOfWeek(sunday))
    }

    @Test
    fun mapsEveryMidweekDayBackToTheSameSaturday() {
        assertEquals(saturday, WeekBoundary.startOfWeek(monday))
        assertEquals(saturday, WeekBoundary.startOfWeek(tuesday))
        assertEquals(saturday, WeekBoundary.startOfWeek(wednesday))
        assertEquals(saturday, WeekBoundary.startOfWeek(thursday))
    }

    @Test
    fun keepsFridayInTheWeekThatIsEnding() {
        // Friday is the last day of the week, not the first day of the next one.
        assertEquals(saturday, WeekBoundary.startOfWeek(friday))
    }

    @Test
    fun startsAFreshWeekOnTheFollowingSaturday() {
        assertEquals(nextSaturday, WeekBoundary.startOfWeek(nextSaturday))
    }

    @Test
    fun isIdempotent() {
        // Feeding a start back in must not walk another week backwards.
        assertEquals(
            WeekBoundary.startOfWeek(thursday),
            WeekBoundary.startOfWeek(WeekBoundary.startOfWeek(thursday)),
        )
    }

    // --- the end of the week -------------------------------------------------------------

    @Test
    fun endsTheWeekOnTheFollowingFriday() {
        assertEquals(friday, WeekBoundary.endOfWeek(thursday))
        assertEquals(friday, WeekBoundary.endOfWeek(saturday))
        assertEquals(friday, WeekBoundary.endOfWeek(friday))
    }

    @Test
    fun listsSevenConsecutiveDaysStartingAtSaturday() {
        val days = WeekBoundary.daysOfWeek(wednesday)

        assertEquals(
            listOf(saturday, sunday, monday, tuesday, wednesday, thursday, friday),
            days,
        )
    }

    // --- boundaries ------------------------------------------------------------------------

    @Test
    fun crossesAYearEndToFindTheStart() {
        // Thursday 1 January 2026 belongs to the week that began Saturday 27 December 2025.
        assertEquals(LocalDate(2025, 12, 27), WeekBoundary.startOfWeek(LocalDate(2026, 1, 1)))
    }

    @Test
    fun crossesAMonthEndToFindTheStart() {
        // Sunday 1 February 2026 belongs to the week that began Saturday 31 January.
        assertEquals(LocalDate(2026, 1, 31), WeekBoundary.startOfWeek(LocalDate(2026, 2, 1)))
    }

    @Test
    fun spansALeapDayWithoutLosingADay() {
        // The week beginning Saturday 24 February 2024 runs through 29 February to 1 March.
        val days = WeekBoundary.daysOfWeek(LocalDate(2024, 2, 29))

        assertEquals(LocalDate(2024, 2, 24), days.first())
        assertEquals(LocalDate(2024, 3, 1), days.last())
        assertEquals(7, days.size)
    }
}
