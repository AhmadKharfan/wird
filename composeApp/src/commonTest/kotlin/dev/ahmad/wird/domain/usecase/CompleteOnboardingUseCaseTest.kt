package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.fake.FakeSettingsRepository
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.DefaultRoutine
import dev.ahmad.wird.domain.model.NumeralSystem
import dev.ahmad.wird.domain.model.RoutineChoice
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Onboarding ends with one choice — the default routine, or start empty — and no account.
 * Either way the install is finished being set up, and must never be set up again behind
 * the user's back. Today is Thursday 15 January 2026.
 */
class CompleteOnboardingUseCaseTest {

    private val today = LocalDate(2026, 1, 15)

    private val habits = FakeHabitRepository()
    private val settings = FakeSettingsRepository()
    private val seed = SeedDefaultRoutineUseCase(habits, settings, HistoryFixtures.clock, HistoryFixtures.zone)
    private val complete = CompleteOnboardingUseCase(seed, settings)

    @Test
    fun plantsTheDefaultRoutineStartingTodayWhenChosen() = runTest {
        complete(RoutineChoice.DEFAULT_ROUTINE)

        assertEquals(
            DefaultRoutine.habitsFrom(today).map { it.id },
            habits.habits.sortedBy { it.sortOrder }.map { it.id },
        )
        assertEquals(setOf(today), habits.habits.map { it.effectiveFrom }.toSet())
    }

    @Test
    fun plantsNothingWhenStartingEmpty() = runTest {
        complete(RoutineChoice.EMPTY)

        assertEquals(emptyList(), habits.habits)
    }

    @Test
    fun recordsOnboardingAsFinishedWhicheverRoutineIsChosen() = runTest {
        RoutineChoice.entries.forEach { choice ->
            val chosenSettings = FakeSettingsRepository()
            val chosenHabits = FakeHabitRepository()
            val completeWith = CompleteOnboardingUseCase(
                SeedDefaultRoutineUseCase(chosenHabits, chosenSettings, HistoryFixtures.clock, HistoryFixtures.zone),
                chosenSettings,
            )

            completeWith(choice)

            assertTrue(chosenSettings.settings.onboardingCompleted, "after $choice")
        }
    }

    @Test
    fun neverPlantsTheRoutineLaterForAUserWhoStartedEmpty() = runTest {
        // The launch-time seed runs every day after this. Starting empty must survive it.
        complete(RoutineChoice.EMPTY)

        seed()

        assertEquals(emptyList(), habits.habits)
    }

    @Test
    fun plantsOneRoutineEvenIfFinishedTwice() = runTest {
        complete(RoutineChoice.DEFAULT_ROUTINE)
        complete(RoutineChoice.DEFAULT_ROUTINE)

        assertEquals(DefaultRoutine.habitsFrom(today).size, habits.habits.size)
    }

    @Test
    fun leavesOnboardingUnfinishedIfTheRoutineCannotBePlanted() = runTest {
        // Recorded as finished with no routine, the user would land on an empty Today they
        // never chose, and the flag would stop seeding from ever repairing it. Unfinished,
        // onboarding simply runs again.
        habits.controls.failWith(IllegalStateException("disk full"))

        assertFailsWith<IllegalStateException> { complete(RoutineChoice.DEFAULT_ROUTINE) }

        assertFalse(settings.settings.onboardingCompleted)
    }

    @Test
    fun keepsEveryOtherSetting() = runTest {
        val arabicIndic = FakeSettingsRepository(AppSettings.DEFAULTS.copy(numeralSystem = NumeralSystem.ARABIC_INDIC))
        val completeWith = CompleteOnboardingUseCase(
            SeedDefaultRoutineUseCase(habits, arabicIndic, HistoryFixtures.clock, HistoryFixtures.zone),
            arabicIndic,
        )

        completeWith(RoutineChoice.EMPTY)

        assertEquals(NumeralSystem.ARABIC_INDIC, arabicIndic.settings.numeralSystem)
    }
}
