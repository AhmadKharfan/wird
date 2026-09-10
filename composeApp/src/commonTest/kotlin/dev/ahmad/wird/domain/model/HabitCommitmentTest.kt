package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HabitCommitmentTest {

    private val tolerance = 1e-6f

    private val habit = Habit(
        id = "duha",
        name = "صلاة الضحى",
        kind = HabitKind.BOOL,
        target = 1,
        iconKey = "sun",
        sortOrder = 1,
        effectiveFrom = LocalDate(2026, 1, 1),
    )

    private fun commitment(completedDays: Int, elapsedDays: Int) =
        HabitCommitment(habit = habit, completedDays = completedDays, elapsedDays = elapsedDays)

    @Test
    fun scoresAHabitAddedTodayAsZeroRatherThanDividingByZero() {
        // A habit created today has no elapsed days behind it yet.
        assertEquals(0f, commitment(completedDays = 0, elapsedDays = 0).ratio, tolerance)
    }

    @Test
    fun scoresHalfTheDaysAsAHalf() {
        assertEquals(0.5f, commitment(completedDays = 3, elapsedDays = 6).ratio, tolerance)
    }

    @Test
    fun scoresAHabitNeverDoneAsZero() {
        assertEquals(0f, commitment(completedDays = 0, elapsedDays = 10).ratio, tolerance)
    }

    @Test
    fun scoresAHabitNeverMissedAsOne() {
        assertEquals(1f, commitment(completedDays = 10, elapsedDays = 10).ratio, tolerance)
    }

    @Test
    fun rejectsMoreCompletedDaysThanHaveElapsed() {
        // Only days the habit was actually live count as elapsed, so completing more
        // than elapsed means the window was computed wrongly.
        assertFailsWith<IllegalArgumentException> { commitment(completedDays = 8, elapsedDays = 7) }
    }

    @Test
    fun rejectsNegativeCompletedDays() {
        assertFailsWith<IllegalArgumentException> { commitment(completedDays = -1, elapsedDays = 7) }
    }

    @Test
    fun rejectsNegativeElapsedDays() {
        assertFailsWith<IllegalArgumentException> { commitment(completedDays = 0, elapsedDays = -1) }
    }
}
