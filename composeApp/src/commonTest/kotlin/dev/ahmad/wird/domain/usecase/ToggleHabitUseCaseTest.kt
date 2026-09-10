package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A tap means different things to the two habit kinds. A BOOL flips; a COUNTER advances
 * and wraps back to zero once it is at or past its target, which is the only way to undo
 * an over-tap without a second control.
 */
class ToggleHabitUseCaseTest {

    private val day = LocalDate(2026, 1, 15)

    private fun habit(kind: HabitKind, target: Int) = Habit(
        id = "habit",
        name = "habit",
        kind = kind,
        target = target,
        iconKey = "dot",
        sortOrder = 0,
        effectiveFrom = LocalDate(2026, 1, 1),
    )

    private val boolHabit = habit(HabitKind.BOOL, target = 1)
    private val counterHabit = habit(HabitKind.COUNTER, target = 5)

    // --- BOOL ------------------------------------------------------------------------

    @Test
    fun marksAnUndoneBoolHabitAsDone() = runTest {
        val entries = FakeEntryRepository()

        ToggleHabitUseCase(entries)(boolHabit, day, currentValue = 0)

        assertEquals(1, entries.valueOf("habit", day))
    }

    @Test
    fun clearsADoneBoolHabit() = runTest {
        val entries = FakeEntryRepository()
        val toggle = ToggleHabitUseCase(entries)
        toggle(boolHabit, day, currentValue = 0)

        toggle(boolHabit, day, currentValue = 1)

        assertEquals(0, entries.valueOf("habit", day))
    }

    // --- COUNTER ----------------------------------------------------------------------

    @Test
    fun advancesACounterFromZero() = runTest {
        val entries = FakeEntryRepository()

        ToggleHabitUseCase(entries)(counterHabit, day, currentValue = 0)

        assertEquals(1, entries.valueOf("habit", day))
    }

    @Test
    fun advancesACounterInTheMiddle() = runTest {
        val entries = FakeEntryRepository()

        ToggleHabitUseCase(entries)(counterHabit, day, currentValue = 3)

        assertEquals(4, entries.valueOf("habit", day))
    }

    @Test
    fun advancesACounterOntoItsTarget() = runTest {
        // The fifth tap must land on 5, not wrap early.
        val entries = FakeEntryRepository()

        ToggleHabitUseCase(entries)(counterHabit, day, currentValue = 4)

        assertEquals(5, entries.valueOf("habit", day))
    }

    @Test
    fun wrapsACounterBackToZeroOnceItIsAtItsTarget() = runTest {
        val entries = FakeEntryRepository()

        ToggleHabitUseCase(entries)(counterHabit, day, currentValue = 5)

        assertEquals(0, entries.valueOf("habit", day))
    }

    @Test
    fun wrapsACounterHoldingMoreThanItsCurrentTarget() = runTest {
        // The target was lowered from 8 to 5 after this value was recorded. Advancing
        // would take it to 7 and strand it above the target forever.
        val entries = FakeEntryRepository()

        ToggleHabitUseCase(entries)(counterHabit, day, currentValue = 6)

        assertEquals(0, entries.valueOf("habit", day))
    }

    @Test
    fun rejectsANegativeCurrentValue() = runTest {
        val entries = FakeEntryRepository()

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            ToggleHabitUseCase(entries)(counterHabit, day, currentValue = -1)
        }
    }
}
