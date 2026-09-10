package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.fake.FakeHabitRepository
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
 * The week runs Saturday 10 January 2026 to Friday the 16th. Every test below feeds in
 * some day of that week and expects the same seven days back.
 */
class ObserveWeekSummaryUseCaseTest {

    private val tolerance = 1e-6f

    private val saturday = LocalDate(2026, 1, 10)
    private val wednesday = LocalDate(2026, 1, 14)
    private val friday = LocalDate(2026, 1, 16)

    private fun jan(day: Int) = LocalDate(2026, 1, day)

    private fun habit(
        id: String = "duha",
        kind: HabitKind = HabitKind.BOOL,
        target: Int = 1,
        effectiveFrom: LocalDate = LocalDate(2026, 1, 1),
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

    private fun entry(habitId: String, day: LocalDate, value: Int = 1) = Entry(
        id = "$habitId@$day",
        habitId = habitId,
        day = day,
        value = value,
        updatedAt = Instant.fromEpochSeconds(1_768_000_000),
    )

    private fun useCase(habits: FakeHabitRepository, entries: FakeEntryRepository) =
        ObserveWeekSummaryUseCase(habits, entries, CalculateDayStatsUseCase())

    @Test
    fun coversSaturdayThroughFridayWhateverDayIsAskedFor() = runTest {
        val habits = FakeHabitRepository(listOf(habit()))

        val summary = useCase(habits, FakeEntryRepository())(wednesday).first()

        assertEquals(saturday, summary.startDay)
        assertEquals(friday, summary.endDay)
        assertEquals(7, summary.days.size)
        assertEquals(saturday, summary.days.first().day)
        assertEquals(friday, summary.days.last().day)
    }

    @Test
    fun givesTheSameWeekForItsSaturdayAndItsFriday() = runTest {
        val habits = FakeHabitRepository(listOf(habit()))
        val observe = useCase(habits, FakeEntryRepository())

        assertEquals(observe(saturday).first(), observe(friday).first())
    }

    @Test
    fun scoresTheDaysOfTheWeekFromTheirOwnEntries() = runTest {
        // duha done on the Sunday and the Wednesday: two of seven days complete.
        val habits = FakeHabitRepository(listOf(habit()))
        val entries = FakeEntryRepository(listOf(entry("duha", jan(11)), entry("duha", jan(14))))

        val summary = useCase(habits, entries)(wednesday).first()

        assertEquals(2, summary.points)
        assertEquals(7, summary.maxPoints)
        assertEquals(2, summary.completeDays)
    }

    @Test
    fun ignoresEntriesFromOutsideTheWeek() = runTest {
        // The 9th is the Friday that closed the previous week; the 17th opens the next.
        val habits = FakeHabitRepository(listOf(habit()))
        val entries = FakeEntryRepository(listOf(entry("duha", jan(9)), entry("duha", jan(17))))

        val summary = useCase(habits, entries)(wednesday).first()

        assertEquals(0, summary.points)
        assertEquals(0, summary.completeDays)
    }

    @Test
    fun shrinksTheMaximumOnlyFromTheDayAHabitIsRetired() = runTest {
        // Retired on the Wednesday: Saturday through Tuesday still expect it, the
        // Wednesday onward does not. Four days of two habits plus three days of one.
        val habits = FakeHabitRepository(
            listOf(habit("duha"), habit("witr", retiredOn = wednesday)),
        )

        val summary = useCase(habits, FakeEntryRepository())(wednesday).first()

        assertEquals(11, summary.maxPoints)
    }

    @Test
    fun summarisesAnEmptyWeekAsZeroRatherThanNothing() = runTest {
        val summary = useCase(FakeHabitRepository(), FakeEntryRepository())(wednesday).first()

        assertEquals(7, summary.days.size)
        assertEquals(0, summary.points)
        assertEquals(0, summary.maxPoints)
        assertEquals(0f, summary.ratio, tolerance)
    }

    @Test
    fun spansTheEndOfAYear() = runTest {
        // Thursday 1 January 2026 sits in the week that began Saturday 27 December 2025.
        val habits = FakeHabitRepository(listOf(habit()))

        val summary = useCase(habits, FakeEntryRepository())(LocalDate(2026, 1, 1)).first()

        assertEquals(LocalDate(2025, 12, 27), summary.startDay)
        assertEquals(LocalDate(2026, 1, 2), summary.endDay)
    }
}
