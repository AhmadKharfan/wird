package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.model.DayStats
import dev.ahmad.wird.domain.model.Entry
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Scoring reads only the snapshot it is given. That is what makes the two history rules
 * hold: a target edit and a deactivation both change which habits and targets a *future*
 * snapshot carries, and neither can reach a snapshot that was already built for a past
 * day. The last two sections here walk both rules end to end.
 */
class CalculateDayStatsUseCaseTest {

    private val jan10 = LocalDate(2026, 1, 10)
    private val jan12 = LocalDate(2026, 1, 12)
    private val jan14 = LocalDate(2026, 1, 14)
    private val jan15 = LocalDate(2026, 1, 15)

    private val calculate = CalculateDayStatsUseCase()

    private fun habit(
        id: String,
        kind: HabitKind = HabitKind.BOOL,
        target: Int = 1,
        sortOrder: Int = 0,
        effectiveFrom: LocalDate = LocalDate(2026, 1, 1),
        retiredOn: LocalDate? = null,
    ) = Habit(
        id = id,
        name = id,
        kind = kind,
        target = target,
        iconKey = "dot",
        sortOrder = sortOrder,
        effectiveFrom = effectiveFrom,
        retiredOn = retiredOn,
    )

    private fun entry(habitId: String, day: LocalDate, value: Int) = Entry(
        id = "$habitId@$day",
        habitId = habitId,
        day = day,
        value = value,
        updatedAt = Instant.fromEpochSeconds(1_768_000_000),
    )

    private val prayers = habit("prayers", HabitKind.COUNTER, target = 5, sortOrder = 0)
    private val duha = habit("duha", sortOrder = 1)
    private val witr = habit("witr", sortOrder = 2)

    // --- the empty day ----------------------------------------------------------------

    @Test
    fun scoresADayWithNothingScheduledAsEmpty() {
        val stats = calculate(DaySnapshot(jan15, habits = emptyList(), entries = emptyList()))

        assertEquals(DayStats.EMPTY, stats)
        assertEquals(0f, stats.ratio, 1e-6f)
        assertFalse(stats.isComplete)
    }

    @Test
    fun scoresAnUntouchedRoutineAsZeroOfItsMaximum() {
        val stats = calculate(
            DaySnapshot(jan15, habits = listOf(prayers, duha, witr), entries = emptyList()),
        )

        assertEquals(0, stats.points)
        assertEquals(7, stats.maxPoints)
        assertFalse(stats.isComplete)
    }

    // --- ordinary scoring ----------------------------------------------------------------

    @Test
    fun countsEachCounterRepetitionAsAPoint() {
        // 3 prayers plus duha done is 4 of a possible 7, derived by hand.
        val stats = calculate(
            DaySnapshot(
                day = jan15,
                habits = listOf(prayers, duha, witr),
                entries = listOf(entry("prayers", jan15, 3), entry("duha", jan15, 1)),
            ),
        )

        assertEquals(4, stats.points)
        assertEquals(7, stats.maxPoints)
    }

    @Test
    fun completesADayWhenEveryHabitReachesItsTarget() {
        val stats = calculate(
            DaySnapshot(
                day = jan15,
                habits = listOf(prayers, duha, witr),
                entries = listOf(
                    entry("prayers", jan15, 5),
                    entry("duha", jan15, 1),
                    entry("witr", jan15, 1),
                ),
            ),
        )

        assertEquals(7, stats.points)
        assertEquals(7, stats.maxPoints)
        assertTrue(stats.isComplete)
    }

    @Test
    fun clampsACounterToItsTarget() {
        // Storage keeps an over-tap; scoring must not, or a single habit could carry the
        // whole day past its maximum and DayStats would reject the result outright.
        val stats = calculate(
            DaySnapshot(
                day = jan15,
                habits = listOf(prayers),
                entries = listOf(entry("prayers", jan15, 7)),
            ),
        )

        assertEquals(5, stats.points)
        assertEquals(5, stats.maxPoints)
    }

    @Test
    fun ignoresAnEntryForAHabitNotScheduledThatDay() {
        // A habit retired yesterday can still have an entry from a day it was live.
        val stats = calculate(
            DaySnapshot(
                day = jan15,
                habits = listOf(duha, habit("gone", retiredOn = jan14)),
                entries = listOf(entry("gone", jan15, 1)),
            ),
        )

        assertEquals(0, stats.points)
        assertEquals(1, stats.maxPoints)
    }

    // --- the target-change rule, end to end ------------------------------------------------

    @Test
    fun scoresAPastDayAgainstTheTargetThatWasInForceThen() {
        // "الصلوات الخمس" was a target of 3 until the 12th, then 5. Three prayers on the
        // 10th finished that day and must still read as finished.
        val oldRevision = habit("prayers", HabitKind.COUNTER, target = 3, retiredOn = jan12)
        val newRevision = habit("prayers", HabitKind.COUNTER, target = 5, effectiveFrom = jan12)
        val revisions = listOf(oldRevision, newRevision)

        val past = calculate(
            DaySnapshot(jan10, habits = revisions, entries = listOf(entry("prayers", jan10, 3))),
        )

        assertEquals(3, past.points)
        assertEquals(3, past.maxPoints)
        assertTrue(past.isComplete)
    }

    @Test
    fun scoresATodayAgainstTheNewTarget() {
        // The same two revisions, the same three prayers, a later day: now it is 3 of 5.
        val revisions = listOf(
            habit("prayers", HabitKind.COUNTER, target = 3, retiredOn = jan12),
            habit("prayers", HabitKind.COUNTER, target = 5, effectiveFrom = jan12),
        )

        val today = calculate(
            DaySnapshot(jan15, habits = revisions, entries = listOf(entry("prayers", jan15, 3))),
        )

        assertEquals(3, today.points)
        assertEquals(5, today.maxPoints)
        assertFalse(today.isComplete)
    }

    // --- the deactivation rule, end to end ---------------------------------------------------

    @Test
    fun keepsADeactivatedHabitInTheMaximumOfDaysItWasLive() {
        val retired = habit("witr", retiredOn = jan15)

        val before = calculate(
            DaySnapshot(jan14, habits = listOf(duha, retired), entries = listOf(entry("duha", jan14, 1))),
        )

        assertEquals(1, before.points)
        assertEquals(2, before.maxPoints)
        assertFalse(before.isComplete)
    }

    @Test
    fun dropsADeactivatedHabitFromTheMaximumFromItsRetirementDay() {
        // The same routine one day later: witr is gone, so duha alone finishes the day.
        val retired = habit("witr", retiredOn = jan15)

        val after = calculate(
            DaySnapshot(jan15, habits = listOf(duha, retired), entries = listOf(entry("duha", jan15, 1))),
        )

        assertEquals(1, after.points)
        assertEquals(1, after.maxPoints)
        assertTrue(after.isComplete)
    }
}
