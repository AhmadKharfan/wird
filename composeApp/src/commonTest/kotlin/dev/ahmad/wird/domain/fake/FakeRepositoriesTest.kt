package dev.ahmad.wird.domain.fake

import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import dev.ahmad.wird.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * The fakes are a deliverable, not scaffolding: every ViewModel from here on is tested
 * against them and never against Room. So the two things they promise beyond storing data
 * — that they can be made slow, and made to fail — are themselves tested.
 */
class FakeRepositoriesTest {

    private val day = LocalDate(2026, 1, 15)

    private fun habit(id: String = "duha") = Habit(
        id = id,
        name = id,
        kind = HabitKind.BOOL,
        target = 1,
        iconKey = id,
        sortOrder = 0,
        effectiveFrom = LocalDate(2026, 1, 1),
    )

    // --- failure ---------------------------------------------------------------------

    @Test
    fun failsAWriteOnDemand() = runTest {
        val entries = FakeEntryRepository()
        entries.controls.failWith(IllegalStateException("disk is full"))

        assertFailsWith<IllegalStateException> { entries.setValue("duha", day, 1) }
    }

    @Test
    fun leavesStateUntouchedWhenAWriteFails() = runTest {
        // The rollback path a ViewModel has to implement only means something if the
        // failed write really did not land.
        val entries = FakeEntryRepository()
        entries.controls.failWith(IllegalStateException("disk is full"))

        assertFailsWith<IllegalStateException> { entries.setValue("duha", day, 1) }

        assertEquals(emptyList(), entries.entries)
    }

    @Test
    fun failsExactlyOnceWhenAskedTo() = runTest {
        // The shape a Today test needs: one tap fails and rolls back, the next succeeds.
        val entries = FakeEntryRepository()
        entries.controls.failNextCallWith(IllegalStateException("transient"))

        assertFailsWith<IllegalStateException> { entries.setValue("duha", day, 1) }
        entries.setValue("duha", day, 1)

        assertEquals(1, entries.valueOf("duha", day))
    }

    @Test
    fun stopsFailingWhenToldToSucceed() = runTest {
        val habits = FakeHabitRepository()
        habits.controls.failWith(IllegalStateException("nope"))
        assertFailsWith<IllegalStateException> { habits.upsert(habit()) }

        habits.controls.succeed()
        habits.upsert(habit())

        assertEquals(listOf("duha"), habits.habits.map { it.id })
    }

    @Test
    fun failsReadsAsWellAsWrites() = runTest {
        val settings = FakeSettingsRepository()
        settings.controls.failWith(IllegalStateException("nope"))

        assertFailsWith<IllegalStateException> { settings.observeSettings().first() }
    }

    // --- delay --------------------------------------------------------------------------

    @Test
    fun answersSlowlyWhenAskedTo() = runTest {
        // A ViewModel that shows a spinner, or one that must not, needs a call that
        // actually takes time. runTest's virtual clock makes it free.
        val entries = FakeEntryRepository()
        entries.controls.delay = 3.seconds

        val before = currentTime
        entries.setValue("duha", day, 1)

        assertEquals(3_000, currentTime - before)
    }

    @Test
    fun answersInstantlyByDefault() = runTest {
        val entries = FakeEntryRepository()

        val before = currentTime
        entries.setValue("duha", day, 1)

        assertEquals(0, currentTime - before)
    }

    // --- behaving like the real thing ------------------------------------------------------

    @Test
    fun keepsAnEntrysIdStableAcrossEdits() = runTest {
        // The real repository guarantees this, so the fake must too — otherwise a test
        // passes here and the same code fails against Room.
        val entries = FakeEntryRepository()
        entries.setValue("duha", day, 1)
        val originalId = entries.entries.single().id

        entries.setValue("duha", day, 0)

        assertEquals(originalId, entries.entries.single().id)
    }

    @Test
    fun keepsOneEntryPerHabitPerDay() = runTest {
        val entries = FakeEntryRepository()

        entries.setValue("duha", day, 1)
        entries.setValue("duha", day, 0)

        assertEquals(1, entries.entries.size)
    }

    @Test
    fun closesTheOpenRevisionWhenAHabitIsRevised() = runTest {
        val habits = FakeHabitRepository()
        habits.upsert(habit().copy(effectiveFrom = LocalDate(2026, 1, 1)))

        habits.upsert(habit().copy(effectiveFrom = day))

        assertEquals(2, habits.habits.size)
        assertEquals(1, habits.observeHabitsOn(day).first().size)
    }

    @Test
    fun startsSettingsAtTheDefaults() = runTest {
        assertEquals(AppSettings.DEFAULTS, FakeSettingsRepository().observeSettings().first())
    }

    @Test
    fun appliesASettingsTransform() = runTest {
        val settings = FakeSettingsRepository()

        settings.update { it.copy(themeMode = ThemeMode.DARK) }

        assertEquals(ThemeMode.DARK, settings.observeSettings().first().themeMode)
    }

    @Test
    fun reportsNoHabitsUntilOneExists() = runTest {
        val habits = FakeHabitRepository()
        assertTrue(habits.habits.isEmpty())

        habits.upsert(habit())

        assertTrue(habits.hasAnyHabit())
    }
}
