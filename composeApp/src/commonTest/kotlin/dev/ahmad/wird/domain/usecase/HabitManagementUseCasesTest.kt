package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.usecase.HistoryFixtures.habit
import dev.ahmad.wird.domain.usecase.HistoryFixtures.today
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Reordering and deactivating are thin on purpose: the rules they depend on were settled in
 * the data layer, and the job here is to honour them rather than reinvent them. So these
 * tests read back through the same observers the screens use, to prove the rule still
 * holds one layer up.
 */
class HabitManagementUseCasesTest {

    private val yesterday = LocalDate(2026, 1, 14)

    private fun routine() = FakeHabitRepository(
        listOf(habit("a", sortOrder = 0), habit("b", sortOrder = 1), habit("c", sortOrder = 2)),
    )

    // --- reordering --------------------------------------------------------------------------

    @Test
    fun persistsTheNewOrder() = runTest {
        val habits = routine()

        ReorderHabitsUseCase(habits)(listOf("c", "a", "b"))

        assertEquals(listOf("c", "a", "b"), habits.observeActiveHabits().first().map { it.id })
    }

    @Test
    fun rejectsAnOrderThatNamesAHabitTwiceAndChangesNothing() = runTest {
        // A duplicate would give one habit two positions and let the later one silently
        // win, so the drop target the user saw would not be where the habit landed.
        val habits = routine()

        assertFailsWith<IllegalArgumentException> {
            ReorderHabitsUseCase(habits)(listOf("a", "a", "b"))
        }

        assertEquals(listOf("a", "b", "c"), habits.observeActiveHabits().first().map { it.id })
    }

    // --- deactivating: the phase 2 rule, one layer up -------------------------------------------

    private fun setActive(habits: FakeHabitRepository) =
        SetHabitActiveUseCase(habits, HistoryFixtures.clock, HistoryFixtures.zone)

    private fun observeDay(habits: FakeHabitRepository) = ObserveDayUseCase(habits, FakeEntryRepository())

    @Test
    fun dropsADeactivatedHabitFromToday() = runTest {
        val habits = routine()

        setActive(habits)("b", active = false)

        assertEquals(listOf("a", "c"), observeDay(habits)(today).first().scheduledHabits.map { it.id })
    }

    @Test
    fun keepsADeactivatedHabitOnEveryEarlierDay() = runTest {
        val habits = routine()

        setActive(habits)("b", active = false)

        assertEquals(
            listOf("a", "b", "c"),
            observeDay(habits)(yesterday).first().scheduledHabits.map { it.id },
        )
    }

    @Test
    fun bringsADeactivatedHabitBack() = runTest {
        val habits = routine()
        val set = setActive(habits)
        set("b", active = false)

        set("b", active = true)

        assertEquals(listOf("a", "b", "c"), habits.observeActiveHabits().first().map { it.id })
    }

    // --- listing -------------------------------------------------------------------------------

    @Test
    fun listsActiveHabitsInTheirOrderAndLeavesRetiredOnesOut() = runTest {
        val habits = FakeHabitRepository(
            listOf(
                habit("second", sortOrder = 1),
                habit("first", sortOrder = 0),
                habit("gone", sortOrder = 2, retiredOn = yesterday),
            ),
        )

        assertEquals(
            listOf("first", "second"),
            ObserveActiveHabitsUseCase(habits)().first().map { it.id },
        )
    }
}
