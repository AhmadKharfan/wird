package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

/**
 * A snapshot is the unit of scoring: it carries the habits as they stood on [day], never
 * the habits as they stand now. Everything downstream reads [DaySnapshot.scheduledHabits],
 * which is what keeps a target edit or a deactivation from rescoring the past.
 */
class DaySnapshotTest {

    private val day = LocalDate(2026, 1, 15)

    private fun habit(
        id: String,
        sortOrder: Int = 0,
        effectiveFrom: LocalDate = LocalDate(2026, 1, 1),
        retiredOn: LocalDate? = null,
        target: Int = 1,
        kind: HabitKind = HabitKind.BOOL,
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

    private fun entry(
        habitId: String,
        value: Int = 1,
        entryDay: LocalDate = day,
        updatedAt: Instant = Instant.fromEpochSeconds(1_768_000_000),
    ) = Entry(
        id = "$habitId@$entryDay",
        habitId = habitId,
        day = entryDay,
        value = value,
        updatedAt = updatedAt,
    )

    // --- which habits count on this day -------------------------------------------

    @Test
    fun excludesAHabitThatHadNotStartedYet() {
        val snapshot = DaySnapshot(
            day = day,
            habits = listOf(habit("later", effectiveFrom = LocalDate(2026, 2, 1))),
            entries = emptyList(),
        )

        assertEquals(emptyList<Habit>(), snapshot.scheduledHabits)
    }

    @Test
    fun excludesAHabitRetiredBeforeThisDay() {
        val snapshot = DaySnapshot(
            day = day,
            habits = listOf(habit("gone", retiredOn = LocalDate(2026, 1, 10))),
            entries = emptyList(),
        )

        assertEquals(emptyList<Habit>(), snapshot.scheduledHabits)
    }

    @Test
    fun includesAHabitRetiredAfterThisDay() {
        // The deactivation rule seen from the other side: history keeps the habit.
        val snapshot = DaySnapshot(
            day = day,
            habits = listOf(habit("kept", retiredOn = LocalDate(2026, 1, 20))),
            entries = emptyList(),
        )

        assertEquals(listOf("kept"), snapshot.scheduledHabits.map { it.id })
    }

    @Test
    fun ordersScheduledHabitsBySortOrderNotByInputOrder() {
        // The Today list is rendered straight from this, so input order must not leak.
        val snapshot = DaySnapshot(
            day = day,
            habits = listOf(habit("third", sortOrder = 2), habit("first", sortOrder = 0), habit("second", sortOrder = 1)),
            entries = emptyList(),
        )

        assertEquals(listOf("first", "second", "third"), snapshot.scheduledHabits.map { it.id })
    }

    @Test
    fun keepsOnlyTheRevisionLiveOnThisDayWhenAHabitWasRevised() {
        // A target edit leaves two revisions sharing an id. Counting both would double
        // the habit's contribution to the day's maximum.
        val snapshot = DaySnapshot(
            day = day,
            habits = listOf(
                habit("prayers", kind = HabitKind.COUNTER, target = 3, effectiveFrom = LocalDate(2026, 1, 1), retiredOn = LocalDate(2026, 1, 12)),
                habit("prayers", kind = HabitKind.COUNTER, target = 5, effectiveFrom = LocalDate(2026, 1, 12)),
            ),
            entries = emptyList(),
        )

        assertEquals(listOf(5), snapshot.scheduledHabits.map { it.target })
    }

    // --- reading values ------------------------------------------------------------

    @Test
    fun readsZeroForAHabitWithNoEntry() {
        // The empty-day rule: absent means zero, never null and never a crash.
        val snapshot = DaySnapshot(day = day, habits = listOf(habit("duha")), entries = emptyList())

        assertEquals(0, snapshot.valueFor("duha"))
    }

    @Test
    fun readsZeroForAHabitThatDoesNotExistAtAll() {
        val snapshot = DaySnapshot(day = day, habits = emptyList(), entries = emptyList())

        assertEquals(0, snapshot.valueFor("nothing-like-this"))
    }

    @Test
    fun readsTheRecordedValue() {
        val snapshot = DaySnapshot(
            day = day,
            habits = listOf(habit("prayers", kind = HabitKind.COUNTER, target = 5)),
            entries = listOf(entry("prayers", value = 3)),
        )

        assertEquals(3, snapshot.valueFor("prayers"))
    }

    @Test
    fun readsEntriesRecordedAtAnyInstantOfTheDay() {
        // The timezone rule. These two instants are eleven hours apart and fall on
        // different UTC days, but both entries carry this local day, so both belong to
        // this snapshot. Regrouping by updatedAt would drop one of them.
        val snapshot = DaySnapshot(
            day = day,
            habits = listOf(habit("morning"), habit("night")),
            entries = listOf(
                entry("morning", updatedAt = Instant.parse("2026-01-14T20:30:00Z")),
                entry("night", updatedAt = Instant.parse("2026-01-15T07:30:00Z")),
            ),
        )

        assertEquals(1, snapshot.valueFor("morning"))
        assertEquals(1, snapshot.valueFor("night"))
    }

    // --- invariants ----------------------------------------------------------------

    @Test
    fun rejectsAnEntryBelongingToAnotherDay() {
        assertFailsWith<IllegalArgumentException> {
            DaySnapshot(
                day = day,
                habits = listOf(habit("duha")),
                entries = listOf(entry("duha", entryDay = LocalDate(2026, 1, 14))),
            )
        }
    }

    @Test
    fun rejectsTwoEntriesForTheSameHabit() {
        // One habit, one day, one value: a duplicate would make valueFor arbitrary.
        assertFailsWith<IllegalArgumentException> {
            DaySnapshot(
                day = day,
                habits = listOf(habit("duha")),
                entries = listOf(entry("duha", value = 1), entry("duha", value = 0)),
            )
        }
    }

    @Test
    fun rejectsTwoRevisionsOfTheSameHabitBeingLiveAtOnce() {
        assertFailsWith<IllegalArgumentException> {
            DaySnapshot(
                day = day,
                habits = listOf(
                    habit("prayers", kind = HabitKind.COUNTER, target = 3),
                    habit("prayers", kind = HabitKind.COUNTER, target = 5),
                ),
                entries = emptyList(),
            )
        }
    }

    @Test
    fun acceptsACompletelyEmptyDay() {
        val snapshot = DaySnapshot(day = day, habits = emptyList(), entries = emptyList())

        assertEquals(emptyList<Habit>(), snapshot.scheduledHabits)
    }
}
