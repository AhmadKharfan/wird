package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.fake.FakeSettingsRepository
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.DefaultRoutine
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Seeding runs on every launch and must plant the routine exactly once. "Once" is decided
 * by whether any habit has *ever* existed, not by whether any is currently active —
 * otherwise a user who retired everything would find the whole routine back the next
 * morning.
 */
class SeedDefaultRoutineUseCaseTest {

    private val seedDay = LocalDate(2026, 1, 15)

    private fun habit(id: String, retiredOn: LocalDate? = null) = Habit(
        id = id,
        name = id,
        kind = HabitKind.BOOL,
        target = 1,
        iconKey = id,
        sortOrder = 0,
        effectiveFrom = LocalDate(2026, 1, 1),
        retiredOn = retiredOn,
    )

    @Test
    fun plantsTheWholeRoutineOnAnEmptyInstall() = runTest {
        val habits = FakeHabitRepository()

        SeedDefaultRoutineUseCase(habits, FakeSettingsRepository())(seedDay)

        assertEquals(
            DefaultRoutine.habitsFrom(seedDay).map { it.name },
            habits.habits.sortedBy { it.sortOrder }.map { it.name },
        )
    }

    @Test
    fun startsTheRoutineOnTheDayItSeeds() = runTest {
        val habits = FakeHabitRepository()

        SeedDefaultRoutineUseCase(habits, FakeSettingsRepository())(seedDay)

        assertEquals(setOf(seedDay), habits.habits.map { it.effectiveFrom }.toSet())
    }

    @Test
    fun leavesAnInstallThatAlreadyHasHabitsAlone() = runTest {
        val habits = FakeHabitRepository(listOf(habit("mine")))

        SeedDefaultRoutineUseCase(habits, FakeSettingsRepository())(seedDay)

        assertEquals(listOf("mine"), habits.habits.map { it.id })
    }

    @Test
    fun plantsNothingExtraWhenRunTwice() = runTest {
        // Seeding is called on every launch, so running it again must be a no-op rather
        // than a second routine.
        val habits = FakeHabitRepository()
        val seed = SeedDefaultRoutineUseCase(habits, FakeSettingsRepository())

        seed(seedDay)
        seed(LocalDate(2026, 1, 16))

        assertEquals(11, habits.habits.size)
    }

    @Test
    fun doesNotBringBackARoutineTheUserRetired() = runTest {
        // Every habit retired: none is active, but they all still exist, so the install
        // is not a fresh one and must be left as the user left it.
        val retired = DefaultRoutine.habitsFrom(seedDay).map { it.copy(retiredOn = seedDay) }
        val habits = FakeHabitRepository(retired)

        SeedDefaultRoutineUseCase(habits, FakeSettingsRepository())(LocalDate(2026, 2, 1))

        assertEquals(11, habits.habits.size)
        assertEquals(emptyList(), habits.observeActiveHabits().first())
    }

    @Test
    fun plantsNothingForAUserWhoFinishedOnboardingWithNoRoutine() = runTest {
        // Starting empty leaves no habit behind, so "no habit has ever existed" alone would
        // hand this user the default routine on their next launch. Finishing onboarding is
        // the decision, whatever it planted.
        val habits = FakeHabitRepository()
        val onboarded = FakeSettingsRepository(AppSettings.DEFAULTS.copy(onboardingCompleted = true))

        SeedDefaultRoutineUseCase(habits, onboarded)(seedDay)

        assertEquals(emptyList(), habits.habits)
    }

    @Test
    fun leavesTheSeededRoutineWorthFifteenPointsADay() = runTest {
        val habits = FakeHabitRepository()

        SeedDefaultRoutineUseCase(habits, FakeSettingsRepository())(seedDay)

        assertEquals(15, habits.observeActiveHabits().first().sumOf { it.target })
    }
}
