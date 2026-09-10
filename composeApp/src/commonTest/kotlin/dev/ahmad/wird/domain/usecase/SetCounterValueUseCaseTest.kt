package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Setting a counter directly is the long-press path: the user names the number instead of
 * tapping up to it. The offered range is `0..target`, and this use case is what makes
 * that range a contract rather than a UI convention.
 */
class SetCounterValueUseCaseTest {

    private val day = LocalDate(2026, 1, 15)

    private val counterHabit = Habit(
        id = "prayers",
        name = "الصلوات الخمس",
        kind = HabitKind.COUNTER,
        target = 5,
        iconKey = "mosque",
        sortOrder = 0,
        effectiveFrom = LocalDate(2026, 1, 1),
    )

    private val boolHabit = counterHabit.copy(id = "duha", kind = HabitKind.BOOL, target = 1)

    @Test
    fun storesAValueInsideTheRange() = runTest {
        val entries = FakeEntryRepository()

        SetCounterValueUseCase(entries)(counterHabit, day, value = 3)

        assertEquals(3, entries.valueOf("prayers", day))
    }

    @Test
    fun storesZero() = runTest {
        // Zero is "explicitly not done", which must remain distinguishable from
        // "never opened" — so it is stored, not deleted.
        val entries = FakeEntryRepository()

        SetCounterValueUseCase(entries)(counterHabit, day, value = 0)

        assertEquals(1, entries.entries.size)
        assertEquals(0, entries.valueOf("prayers", day))
    }

    @Test
    fun storesExactlyTheTarget() = runTest {
        val entries = FakeEntryRepository()

        SetCounterValueUseCase(entries)(counterHabit, day, value = 5)

        assertEquals(5, entries.valueOf("prayers", day))
    }

    @Test
    fun rejectsAValueAboveTheTarget() = runTest {
        // Six of five prayers is not a thing the user can mean.
        assertFailsWith<IllegalArgumentException> {
            SetCounterValueUseCase(FakeEntryRepository())(counterHabit, day, value = 6)
        }
    }

    @Test
    fun rejectsANegativeValue() = runTest {
        assertFailsWith<IllegalArgumentException> {
            SetCounterValueUseCase(FakeEntryRepository())(counterHabit, day, value = -1)
        }
    }

    @Test
    fun rejectsABoolHabit() = runTest {
        // A BOOL habit has no counter to set; it is toggled. Allowing this would be the
        // one way to store a value other than 0 or 1 against it.
        assertFailsWith<IllegalArgumentException> {
            SetCounterValueUseCase(FakeEntryRepository())(boolHabit, day, value = 1)
        }
    }

    @Test
    fun writesNothingWhenItRejectsTheValue() = runTest {
        val entries = FakeEntryRepository()

        assertFailsWith<IllegalArgumentException> {
            SetCounterValueUseCase(entries)(counterHabit, day, value = 9)
        }

        assertEquals(emptyList(), entries.entries)
    }
}
