package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.model.Entry
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveTodayUseCaseTest {

    /**
     * Late evening in London, already the next afternoon on Kiritimati. Every timezone
     * test below hangs off this one instant.
     */
    private val now = Instant.parse("2026-01-15T22:00:00Z")

    private val london = UtcOffset.ZERO.asTimeZone()
    private val kiritimati = UtcOffset(hours = 14).asTimeZone()

    private val jan15 = LocalDate(2026, 1, 15)
    private val jan16 = LocalDate(2026, 1, 16)

    private fun clockAt(instant: Instant) = object : Clock {
        override fun now(): Instant = instant
    }

    private fun habit(
        id: String,
        sortOrder: Int = 0,
        effectiveFrom: LocalDate = LocalDate(2026, 1, 1),
        retiredOn: LocalDate? = null,
        kind: HabitKind = HabitKind.BOOL,
        target: Int = 1,
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

    private fun entry(habitId: String, day: LocalDate, value: Int = 1) = Entry(
        id = "$habitId@$day",
        habitId = habitId,
        day = day,
        value = value,
        updatedAt = now,
    )

    private fun useCase(
        habits: FakeHabitRepository,
        entries: FakeEntryRepository,
        zone: TimeZone = london,
    ) = ObserveTodayUseCase(ObserveDayUseCase(habits, entries), clockAt(now), zone)

    // --- the ordinary case ------------------------------------------------------------

    @Test
    fun emitsASnapshotOfTheLocalCalendarDay() = runTest {
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(listOf(entry("duha", jan15)))

        val snapshot = useCase(habits, entries)().first()

        assertEquals(jan15, snapshot.day)
        assertEquals(listOf("duha"), snapshot.scheduledHabits.map { it.id })
        assertEquals(1, snapshot.valueFor("duha"))
    }

    @Test
    fun leavesOutAHabitRetiredBeforeToday() = runTest {
        val habits = FakeHabitRepository(listOf(habit("gone", retiredOn = LocalDate(2026, 1, 10))))

        val snapshot = useCase(habits, FakeEntryRepository())().first()

        assertEquals(emptyList(), snapshot.scheduledHabits.map { it.id })
    }

    @Test
    fun keepsAHabitScheduledToRetireTomorrow() = runTest {
        // active is false the moment a retirement is scheduled, but the habit is still
        // in force today. Reading today from observeActiveHabits instead of
        // observeHabitsOn would drop it a day early and shrink today's maximum.
        val habits = FakeHabitRepository(listOf(habit("leaving", retiredOn = jan16)))

        val snapshot = useCase(habits, FakeEntryRepository())().first()

        assertEquals(listOf("leaving"), snapshot.scheduledHabits.map { it.id })
    }

    @Test
    fun emitsAnEmptySnapshotBeforeAnythingIsSetUp() = runTest {
        // The empty-day rule at the use-case level: a first launch emits a real snapshot
        // rather than nothing, an error, or a null.
        val snapshot = useCase(FakeHabitRepository(), FakeEntryRepository())().first()

        assertEquals(jan15, snapshot.day)
        assertEquals(emptyList(), snapshot.scheduledHabits)
        assertEquals(0, snapshot.valueFor("anything"))
    }

    @Test
    fun reflectsAValueWrittenAfterSubscribing() = runTest {
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository()
        val observe = useCase(habits, entries)

        assertEquals(0, observe().first().valueFor("duha"))
        entries.setValue("duha", jan15, 1)

        assertEquals(1, observe().first().valueFor("duha"))
    }

    // --- midnight ------------------------------------------------------------------------

    /** A clock that starts at [start] and moves with the test's virtual time. */
    private fun TestScope.clockFrom(start: Instant) = object : Clock {
        override fun now(): Instant = start + testScheduler.currentTime.milliseconds
    }

    private fun TestScope.daysSeenFrom(start: Instant): List<LocalDate> {
        val observe = ObserveTodayUseCase(
            ObserveDayUseCase(FakeHabitRepository(listOf(habit("duha"))), FakeEntryRepository()),
            clockFrom(start),
            london,
            timer = StandardTestDispatcher(testScheduler),
        )
        val days = mutableListOf<LocalDate>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observe().collect { days += it.day } }
        return days
    }

    @Test
    fun movesOnToTheNextDayAtMidnight() = runTest {
        // Left open overnight, Today has to turn over. Otherwise a tap after midnight is
        // recorded against yesterday, and today looks untouched.
        val days = daysSeenFrom(Instant.parse("2026-01-15T23:59:00Z"))

        advanceTimeBy(2.minutes)

        assertEquals(listOf(jan15, jan16), days.distinct())
    }

    @Test
    fun staysOnTodayUntilMidnight() = runTest {
        val days = daysSeenFrom(Instant.parse("2026-01-15T23:59:00Z"))

        advanceTimeBy(30.seconds)

        assertEquals(listOf(jan15), days.distinct())
    }

    // --- the timezone rule ---------------------------------------------------------------

    @Test
    fun readsTodayFromTheUsersOwnZone() = runTest {
        // Same instant, two zones either side of midnight: "today" differs.
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository()

        assertEquals(jan15, useCase(habits, entries, london)().first().day)
        assertEquals(jan16, useCase(habits, entries, kiritimati)().first().day)
    }

    @Test
    fun leavesAnEntryOnTheDayItWasRecordedWhenTheZoneChanges() = runTest {
        // The rule that matters: flying east moves what "today" means, but it must not
        // drag yesterday's record forward with it. Re-deriving the day from updatedAt
        // would put this entry on the 16th and make the 15th look untouched.
        val habits = FakeHabitRepository(listOf(habit("duha")))
        val entries = FakeEntryRepository(listOf(entry("duha", jan15)))

        val afterFlying = useCase(habits, entries, kiritimati)().first()

        assertEquals(jan16, afterFlying.day)
        assertEquals(0, afterFlying.valueFor("duha"))
        assertEquals(listOf(jan15), entries.entries.map { it.day })
    }
}
