package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.model.BadgeKind
import dev.ahmad.wird.domain.usecase.HistoryFixtures.entry
import dev.ahmad.wird.domain.usecase.HistoryFixtures.habit
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Reads the whole local history — every revision, every entry — from the day the first habit
 * began up to today, and hands it to the calculator. Today is Thursday 15 January 2026.
 */
class LoadBadgesUseCaseTest {

    private fun useCase(habits: FakeHabitRepository, entries: FakeEntryRepository) =
        LoadBadgesUseCase(habits, entries, HistoryFixtures.clock, HistoryFixtures.zone, CalculateBadgesUseCase())

    private fun doneOn(habitId: String, from: LocalDate, count: Int) =
        List(count) { entry(habitId, from.plusDays(it), 1) }

    @Test
    fun readsHistoryFromTheFirstHabitUpToToday() = runTest {
        val habits = FakeHabitRepository(listOf(habit("duha", effectiveFrom = LocalDate(2026, 1, 1))))
        val entries = FakeEntryRepository(doneOn("duha", LocalDate(2026, 1, 1), 7))

        val badges = useCase(habits, entries)()

        assertEquals(LocalDate(2026, 1, 7), badges.single { it.kind == BadgeKind.STREAK_7 }.earnedOn)
    }

    @Test
    fun ignoresDaysRecordedAheadOfToday() = runTest {
        // Flying west can leave entries dated after today. Four days up to today and three
        // after it are not yet a week of anything.
        val habits = FakeHabitRepository(listOf(habit("duha", effectiveFrom = LocalDate(2026, 1, 1))))
        val entries = FakeEntryRepository(doneOn("duha", LocalDate(2026, 1, 12), 7))

        val badges = useCase(habits, entries)()

        assertNull(badges.single { it.kind == BadgeKind.STREAK_7 }.earnedOn)
    }

    @Test
    fun showsEveryBadgeLockedOnAnEmptyInstall() = runTest {
        val badges = useCase(FakeHabitRepository(), FakeEntryRepository())()

        assertEquals(5, badges.size)
        assertEquals(setOf<LocalDate?>(null), badges.map { it.earnedOn }.toSet())
    }
}
