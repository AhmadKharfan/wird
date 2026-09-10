package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.model.Entry
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Commitment judges a habit only over the days it was actually in force. A habit added
 * last week is not marked down for the months before it existed, and a retired one stops
 * accruing misses the day it is retired.
 */
class CalculateHabitCommitmentUseCaseTest {

    private val tolerance = 1e-6f
    private val calculate = CalculateHabitCommitmentUseCase()

    private fun day(n: Int) = LocalDate(2026, 1, n)

    private fun habit(
        id: String = "duha",
        kind: HabitKind = HabitKind.BOOL,
        target: Int = 1,
        effectiveFrom: LocalDate = day(1),
        retiredOn: LocalDate? = null,
    ) = Habit(
        id = id,
        name = id,
        kind = kind,
        target = target,
        iconKey = "dot",
        sortOrder = 0,
        effectiveFrom = effectiveFrom,
        retiredOn = retiredOn,
    )

    /** One snapshot per day in `days`, recording [value] for [habit] where it is non-null. */
    private fun snapshots(habit: Habit, days: List<Int>, values: Map<Int, Int> = emptyMap()) =
        days.map { n ->
            DaySnapshot(
                day = day(n),
                habits = listOf(habit),
                entries = values[n]?.let { value ->
                    listOf(
                        Entry(
                            id = "${habit.id}@${day(n)}",
                            habitId = habit.id,
                            day = day(n),
                            value = value,
                            updatedAt = Instant.fromEpochSeconds(1_768_000_000),
                        ),
                    )
                } ?: emptyList(),
            )
        }

    @Test
    fun scoresAHabitWithNoDaysBehindItAsZero() {
        val subject = habit()

        val commitment = calculate(subject, snapshots(subject, days = emptyList()))

        assertEquals(0, commitment.elapsedDays)
        assertEquals(0, commitment.completedDays)
        assertEquals(0f, commitment.ratio, tolerance)
    }

    @Test
    fun countsTheDaysItWasDoneAgainstTheDaysItWasLive() {
        val subject = habit()

        val commitment = calculate(
            subject,
            snapshots(subject, days = (1..6).toList(), values = mapOf(1 to 1, 3 to 1, 5 to 1)),
        )

        assertEquals(6, commitment.elapsedDays)
        assertEquals(3, commitment.completedDays)
        assertEquals(0.5f, commitment.ratio, tolerance)
    }

    @Test
    fun countsADayWithNoEntryAsElapsedButNotCompleted() {
        val subject = habit()

        val commitment = calculate(subject, snapshots(subject, days = (1..4).toList()))

        assertEquals(4, commitment.elapsedDays)
        assertEquals(0, commitment.completedDays)
    }

    @Test
    fun countsAnExplicitZeroAsElapsedButNotCompleted() {
        val subject = habit()

        val commitment = calculate(
            subject,
            snapshots(subject, days = (1..4).toList(), values = mapOf(2 to 0)),
        )

        assertEquals(4, commitment.elapsedDays)
        assertEquals(0, commitment.completedDays)
    }

    // --- only live days count -------------------------------------------------------------

    @Test
    fun ignoresDaysBeforeTheHabitExisted() {
        // Added on the 4th: the first three days of the range are not its to answer for.
        val subject = habit(effectiveFrom = day(4))

        val commitment = calculate(
            subject,
            snapshots(subject, days = (1..6).toList(), values = mapOf(4 to 1, 5 to 1, 6 to 1)),
        )

        assertEquals(3, commitment.elapsedDays)
        assertEquals(3, commitment.completedDays)
        assertEquals(1f, commitment.ratio, tolerance)
    }

    @Test
    fun ignoresDaysAfterItWasRetired() {
        // Retired on the 4th, so the 4th onward stops counting against it.
        val subject = habit(retiredOn = day(4))

        val commitment = calculate(
            subject,
            snapshots(subject, days = (1..6).toList(), values = mapOf(1 to 1, 2 to 1, 3 to 1)),
        )

        assertEquals(3, commitment.elapsedDays)
        assertEquals(3, commitment.completedDays)
        assertEquals(1f, commitment.ratio, tolerance)
    }

    // --- counters ----------------------------------------------------------------------------

    @Test
    fun countsACounterDayOnlyWhenItReachesItsTarget() {
        val subject = habit(id = "prayers", kind = HabitKind.COUNTER, target = 5)

        val commitment = calculate(
            subject,
            snapshots(subject, days = (1..4).toList(), values = mapOf(1 to 4, 2 to 5, 3 to 6, 4 to 0)),
        )

        // Day 1 falls short; day 2 hits it; day 3 overshoots and still counts.
        assertEquals(4, commitment.elapsedDays)
        assertEquals(2, commitment.completedDays)
        assertEquals(0.5f, commitment.ratio, tolerance)
    }
}
