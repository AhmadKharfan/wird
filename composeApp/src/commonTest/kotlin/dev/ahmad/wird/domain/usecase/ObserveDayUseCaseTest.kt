package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.usecase.HistoryFixtures.entry
import dev.ahmad.wird.domain.usecase.HistoryFixtures.habit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Backs the day-detail sheet, where the user opens a day that is already past.
 *
 * The point of it is that a past day must be scored against the routine as it stood *then*
 * — the same guarantee the Today screen gets for free, extended to a day the user chooses.
 */
class ObserveDayUseCaseTest {

    private val jan10 = LocalDate(2026, 1, 10)
    private val jan15 = LocalDate(2026, 1, 15)

    private fun useCase(habits: FakeHabitRepository, entries: FakeEntryRepository) =
        ObserveDayUseCase(habits, entries)

    @Test
    fun snapshotsTheDayItIsAskedFor() = runTest {
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(listOf(entry("duha", jan10)))

        val snapshot = useCase(habits, entries)(jan10).first()

        assertEquals(jan10, snapshot.day)
        assertEquals(1, snapshot.valueFor("duha"))
    }

    @Test
    fun leavesOutEntriesFromOtherDays() = runTest {
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(listOf(entry("duha", jan15)))

        assertEquals(0, useCase(habits, entries)(jan10).first().valueFor("duha"))
    }

    @Test
    fun scoresAPastDayAgainstTheRoutineThatStoodThen() = runTest {
        // A habit added on the 15th must not appear in the 10th's routine, or every old
        // day would suddenly look like a day something was missed.
        val habits = FakeHabitRepository(
            listOf(habit("duha"), habit("added-later", effectiveFrom = jan15, sortOrder = 1)),
        )

        val snapshot = useCase(habits, FakeEntryRepository())(jan10).first()

        assertEquals(listOf("duha"), snapshot.scheduledHabits.map { it.id })
    }

    @Test
    fun keepsAHabitOnDaysBeforeItWasRetired() = runTest {
        val habits = FakeHabitRepository(listOf(habit("witr", retiredOn = jan15)))

        assertEquals(listOf("witr"), useCase(habits, FakeEntryRepository())(jan10).first().scheduledHabits.map { it.id })
        assertEquals(emptyList(), useCase(habits, FakeEntryRepository())(jan15).first().scheduledHabits)
    }

    @Test
    fun snapshotsADayWithNothingOnItRatherThanFailing() = runTest {
        val snapshot = useCase(FakeHabitRepository(), FakeEntryRepository())(jan10).first()

        assertEquals(jan10, snapshot.day)
        assertEquals(emptyList(), snapshot.scheduledHabits)
        assertEquals(0, snapshot.valueFor("anything"))
    }

    @Test
    fun reflectsAnEditMadeToAPastDay() = runTest {
        // The sheet edits history, so the flow has to show the edit.
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository()
        val observe = useCase(habits, entries)

        assertEquals(0, observe(jan10).first().valueFor("duha"))
        entries.setValue("duha", jan10, 1)

        assertEquals(1, observe(jan10).first().valueFor("duha"))
    }
}
