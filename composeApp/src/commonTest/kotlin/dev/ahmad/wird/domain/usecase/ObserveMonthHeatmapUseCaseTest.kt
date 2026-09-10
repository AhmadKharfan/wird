package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.model.DayStats
import dev.ahmad.wird.domain.model.Entry
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * The heatmap is every day of one calendar month with its score, and nothing else — no
 * grid, no leading blanks. Weeks start on Saturday, so arranging those days into rows is
 * the UI's business; doing it here would put a layout decision in the domain.
 */
class ObserveMonthHeatmapUseCaseTest {

    private fun habit(id: String = "duha") = Habit(
        id = id,
        name = id,
        kind = HabitKind.BOOL,
        target = 1,
        iconKey = "dot",
        sortOrder = 0,
        effectiveFrom = LocalDate(2020, 1, 1),
    )

    private fun entry(habitId: String, day: LocalDate, value: Int = 1) = Entry(
        id = "$habitId@$day",
        habitId = habitId,
        day = day,
        value = value,
        updatedAt = Instant.fromEpochSeconds(1_768_000_000),
    )

    private fun useCase(habits: FakeHabitRepository, entries: FakeEntryRepository) =
        ObserveMonthHeatmapUseCase(habits, entries, CalculateDayStatsUseCase())

    private fun emptyMonth(anyDay: LocalDate) =
        useCase(FakeHabitRepository(), FakeEntryRepository())(anyDay)

    // --- month lengths --------------------------------------------------------------------

    @Test
    fun coversEveryDayOfAThirtyOneDayMonth() = runTest {
        val days = emptyMonth(LocalDate(2026, 1, 15)).first()

        assertEquals(31, days.size)
        assertEquals(LocalDate(2026, 1, 1), days.first().day)
        assertEquals(LocalDate(2026, 1, 31), days.last().day)
    }

    @Test
    fun coversEveryDayOfAThirtyDayMonth() = runTest {
        val days = emptyMonth(LocalDate(2026, 4, 8)).first()

        assertEquals(30, days.size)
        assertEquals(LocalDate(2026, 4, 30), days.last().day)
    }

    @Test
    fun stopsAtTheTwentyEighthInACommonFebruary() = runTest {
        val days = emptyMonth(LocalDate(2026, 2, 3)).first()

        assertEquals(28, days.size)
        assertEquals(LocalDate(2026, 2, 28), days.last().day)
    }

    @Test
    fun includesTheLeapDayInALeapFebruary() = runTest {
        val days = emptyMonth(LocalDate(2024, 2, 3)).first()

        assertEquals(29, days.size)
        assertEquals(LocalDate(2024, 2, 29), days.last().day)
    }

    @Test
    fun coversDecemberWithoutRunningIntoTheNextYear() = runTest {
        // Walking forward until the month changes must notice the year change too.
        val days = emptyMonth(LocalDate(2025, 12, 20)).first()

        assertEquals(31, days.size)
        assertEquals(LocalDate(2025, 12, 31), days.last().day)
    }

    // --- scoring ----------------------------------------------------------------------------

    @Test
    fun scoresEachDayFromItsOwnEntries() = runTest {
        val habits = FakeHabitRepository(listOf(habit()))
        val entries = FakeEntryRepository(
            listOf(entry("duha", LocalDate(2026, 1, 3)), entry("duha", LocalDate(2026, 1, 20))),
        )

        val days = useCase(habits, entries)(LocalDate(2026, 1, 15)).first()

        assertEquals(
            listOf(LocalDate(2026, 1, 3), LocalDate(2026, 1, 20)),
            days.filter { it.stats.isComplete }.map { it.day },
        )
    }

    @Test
    fun ignoresEntriesFromTheMonthsEitherSide() = runTest {
        val habits = FakeHabitRepository(listOf(habit()))
        val entries = FakeEntryRepository(
            listOf(entry("duha", LocalDate(2025, 12, 31)), entry("duha", LocalDate(2026, 2, 1))),
        )

        val days = useCase(habits, entries)(LocalDate(2026, 1, 15)).first()

        assertEquals(0, days.count { it.stats.isComplete })
    }

    @Test
    fun scoresAMonthWithNothingSetUpAsEmptyRatherThanNothing() = runTest {
        val days = emptyMonth(LocalDate(2026, 1, 15)).first()

        assertEquals(List(31) { DayStats.EMPTY }, days.map { it.stats })
    }
}
