package dev.ahmad.wird.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [DayStats.ratio] and [DayStats.isComplete] are derived rather than passed in, so the
 * empty-day rule is stated once here instead of at every call site that builds stats.
 */
class DayStatsTest {

    private val tolerance = 1e-6f

    // --- the empty-day rule ---------------------------------------------------------

    @Test
    fun scoresADayWithNoHabitsAsZeroRatherThanDividingByZero() {
        // A day before the routine was seeded has no habits and so no maximum.
        val stats = DayStats(points = 0, maxPoints = 0)

        assertEquals(0f, stats.ratio, tolerance)
        assertFalse(stats.isComplete)
    }

    @Test
    fun doesNotCallADayWithNoHabitsComplete() {
        // Guarding only the division would leave `0 >= 0` reading as a finished day,
        // which would light up every empty day in the heatmap and extend streaks
        // across gaps.
        assertFalse(DayStats(points = 0, maxPoints = 0).isComplete)
    }

    @Test
    fun emptyIsZeroOutOfZero() {
        assertEquals(0, DayStats.EMPTY.points)
        assertEquals(0, DayStats.EMPTY.maxPoints)
    }

    // --- ordinary scoring ------------------------------------------------------------

    @Test
    fun scoresAnUntouchedDayAsZeroOfItsMaximum() {
        val stats = DayStats(points = 0, maxPoints = 15)

        assertEquals(0f, stats.ratio, tolerance)
        assertFalse(stats.isComplete)
    }

    @Test
    fun scoresAPartialDayAsItsFraction() {
        // 6 of 15 is 0.4, derived by hand.
        assertEquals(0.4f, DayStats(points = 6, maxPoints = 15).ratio, tolerance)
    }

    @Test
    fun scoresAFullDayAsCompleteAtRatioOne() {
        val stats = DayStats(points = 15, maxPoints = 15)

        assertEquals(1f, stats.ratio, tolerance)
        assertTrue(stats.isComplete)
    }

    @Test
    fun isNotCompleteOnePointShort() {
        // The streak walk turns on exactly this boundary.
        assertFalse(DayStats(points = 14, maxPoints = 15).isComplete)
    }

    // --- invariants -------------------------------------------------------------------

    @Test
    fun rejectsNegativePoints() {
        assertFailsWith<IllegalArgumentException> { DayStats(points = -1, maxPoints = 15) }
    }

    @Test
    fun rejectsANegativeMaximum() {
        assertFailsWith<IllegalArgumentException> { DayStats(points = 0, maxPoints = -1) }
    }

    @Test
    fun rejectsScoringAboveTheMaximum() {
        // Scoring clamps each habit to its target, so points can never exceed the
        // maximum. If they ever do, the clamp is broken and this should say so loudly
        // rather than report a ratio above one.
        assertFailsWith<IllegalArgumentException> { DayStats(points = 16, maxPoints = 15) }
    }
}
