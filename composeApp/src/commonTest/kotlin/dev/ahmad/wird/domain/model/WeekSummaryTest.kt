package dev.ahmad.wird.domain.model

import dev.ahmad.wird.domain.util.plusDays
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Saturday 10 January 2026 opens the week that runs to Friday the 16th. */
class WeekSummaryTest {

    private val tolerance = 1e-6f
    private val saturday = LocalDate(2026, 1, 10)

    private fun week(vararg stats: DayStats) = WeekSummary(
        startDay = saturday,
        days = stats.mapIndexed { offset, dayStats ->
            DayScore(day = saturday.plusDays(offset), stats = dayStats)
        },
    )

    private val full = DayStats(points = 15, maxPoints = 15)
    private val half = DayStats(points = 6, maxPoints = 15)
    private val untouched = DayStats(points = 0, maxPoints = 15)

    @Test
    fun addsUpTheWeeksPointsAndMaximum() {
        // 15 + 6 + five untouched days is 21 of 105, derived by hand.
        val summary = week(full, half, untouched, untouched, untouched, untouched, untouched)

        assertEquals(21, summary.points)
        assertEquals(105, summary.maxPoints)
        assertEquals(0.2f, summary.ratio, tolerance)
    }

    @Test
    fun countsOnlyTheDaysThatWereFinished() {
        val summary = week(full, full, half, untouched, untouched, untouched, full)

        assertEquals(3, summary.completeDays)
    }

    @Test
    fun scoresAWeekWithNothingScheduledAsZero() {
        // Seven empty days have no maximum at all; the ratio must not divide by zero.
        val summary = week(*Array(7) { DayStats.EMPTY })

        assertEquals(0, summary.points)
        assertEquals(0, summary.maxPoints)
        assertEquals(0f, summary.ratio, tolerance)
        assertEquals(0, summary.completeDays)
    }

    @Test
    fun endsOnTheFridaySixDaysAfterItStarts() {
        val summary = week(*Array(7) { untouched })

        assertEquals(LocalDate(2026, 1, 16), summary.endDay)
    }

    // --- invariants ---------------------------------------------------------------------

    @Test
    fun rejectsAWeekThatIsNotSevenDaysLong() {
        assertFailsWith<IllegalArgumentException> { week(full, full, full) }
    }

    @Test
    fun rejectsDaysThatDoNotBeginAtTheStartDay() {
        assertFailsWith<IllegalArgumentException> {
            WeekSummary(
                startDay = saturday,
                days = List(7) { DayScore(saturday.plusDays(it + 1), DayStats.EMPTY) },
            )
        }
    }

    @Test
    fun rejectsDaysWithAGapInThem() {
        // A missing day would silently shrink the week's maximum and flatter every ratio.
        val withGap = List(7) { offset ->
            DayScore(saturday.plusDays(if (offset >= 3) offset + 1 else offset), DayStats.EMPTY)
        }

        assertFailsWith<IllegalArgumentException> {
            WeekSummary(startDay = saturday, days = withGap)
        }
    }
}
