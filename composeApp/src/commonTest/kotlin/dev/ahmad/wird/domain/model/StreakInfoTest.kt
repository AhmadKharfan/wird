package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class StreakInfoTest {

    private val day = LocalDate(2026, 1, 15)

    @Test
    fun noneIsZeroWithNoCompletedDay() {
        assertEquals(0, StreakInfo.NONE.current)
        assertEquals(0, StreakInfo.NONE.longest)
        assertNull(StreakInfo.NONE.lastCompleteDay)
    }

    @Test
    fun keepsALongestStreakAfterTheCurrentOneBreaks() {
        // The common shape: the run ended, but the record and the last complete day
        // are still worth showing.
        val broken = StreakInfo(current = 0, longest = 12, lastCompleteDay = day)

        assertEquals(0, broken.current)
        assertEquals(12, broken.longest)
        assertEquals(day, broken.lastCompleteDay)
    }

    @Test
    fun rejectsALongestShorterThanTheCurrentRun() {
        // The current run is itself a run, so it can never beat the record.
        assertFailsWith<IllegalArgumentException> {
            StreakInfo(current = 9, longest = 4, lastCompleteDay = day)
        }
    }

    @Test
    fun rejectsANegativeCurrentRun() {
        assertFailsWith<IllegalArgumentException> {
            StreakInfo(current = -1, longest = 0, lastCompleteDay = null)
        }
    }

    @Test
    fun rejectsACompletedRunWithNoDayToPointAt() {
        // A streak of 3 means three days were finished, so one of them is the last.
        assertFailsWith<IllegalArgumentException> {
            StreakInfo(current = 3, longest = 3, lastCompleteDay = null)
        }
    }

    @Test
    fun rejectsALastCompleteDayWithNoStreakBehindIt() {
        // If a day was ever completed, the longest run is at least one.
        assertFailsWith<IllegalArgumentException> {
            StreakInfo(current = 0, longest = 0, lastCompleteDay = day)
        }
    }
}
