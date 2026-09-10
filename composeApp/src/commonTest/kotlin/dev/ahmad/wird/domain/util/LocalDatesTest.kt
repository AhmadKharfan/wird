package dev.ahmad.wird.domain.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Day arithmetic goes through the epoch-day count rather than the day-of-month field.
 * Every case here is a boundary where incrementing the field instead would be wrong.
 */
class LocalDatesTest {

    @Test
    fun stepsAcrossAMonthEnd() {
        assertEquals(LocalDate(2026, 2, 1), LocalDate(2026, 1, 31).plusDays(1))
    }

    @Test
    fun stepsAcrossAYearEnd() {
        assertEquals(LocalDate(2026, 1, 1), LocalDate(2025, 12, 31).plusDays(1))
    }

    @Test
    fun stepsOntoALeapDay() {
        assertEquals(LocalDate(2024, 2, 29), LocalDate(2024, 2, 28).plusDays(1))
    }

    @Test
    fun skipsALeapDayThatDoesNotExist() {
        // 2026 is not a leap year, so the day after 28 February is 1 March.
        assertEquals(LocalDate(2026, 3, 1), LocalDate(2026, 2, 28).plusDays(1))
    }

    @Test
    fun stepsBackwardsAcrossAYearEnd() {
        assertEquals(LocalDate(2025, 12, 31), LocalDate(2026, 1, 1).minusDays(1))
    }

    @Test
    fun stepsBackwardsAcrossALeapDay() {
        assertEquals(LocalDate(2024, 2, 29), LocalDate(2024, 3, 1).minusDays(1))
    }

    @Test
    fun stepsBySeveralDaysAtOnce() {
        assertEquals(LocalDate(2026, 3, 3), LocalDate(2026, 2, 28).plusDays(3))
    }

    @Test
    fun stepsNowhereForZero() {
        assertEquals(LocalDate(2026, 1, 15), LocalDate(2026, 1, 15).plusDays(0))
    }
}
