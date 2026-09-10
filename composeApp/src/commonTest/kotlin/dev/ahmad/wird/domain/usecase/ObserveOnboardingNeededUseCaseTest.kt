package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.fake.FakeSettingsRepository
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.RoutineChoice
import dev.ahmad.wird.domain.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Onboarding is for an install that has never been set up: onboarding not finished, and no
 * habit ever. Either one alone is not enough — a user who started empty has no habits, and
 * an install from before onboarding existed has no flag.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveOnboardingNeededUseCaseTest {

    private val onboarded = AppSettings.DEFAULTS.copy(onboardingCompleted = true)

    private fun needed(settings: FakeSettingsRepository, habits: FakeHabitRepository) =
        ObserveOnboardingNeededUseCase(settings, habits)()

    @Test
    fun needsOnboardingOnAFreshInstall() = runTest {
        assertTrue(needed(FakeSettingsRepository(), FakeHabitRepository()).first())
    }

    @Test
    fun doesNotNeedOnboardingOnceItIsFinished() = runTest {
        val habits = FakeHabitRepository(listOf(HistoryFixtures.habit("duha")))

        assertFalse(needed(FakeSettingsRepository(onboarded), habits).first())
    }

    @Test
    fun doesNotNeedOnboardingAgainAfterStartingEmpty() = runTest {
        assertFalse(needed(FakeSettingsRepository(onboarded), FakeHabitRepository()).first())
    }

    @Test
    fun doesNotSendAnInstallFromBeforeOnboardingThroughIt() = runTest {
        // Upgraded from a version with no onboarding: no flag, but a routine in use.
        val habits = FakeHabitRepository(listOf(HistoryFixtures.habit("duha")))

        assertFalse(needed(FakeSettingsRepository(), habits).first())
    }

    @Test
    fun stopsNeedingOnboardingTheMomentItIsFinished() = runTest {
        val settings = FakeSettingsRepository()
        val habits = FakeHabitRepository()
        val answers = mutableListOf<Boolean>()
        val watching = launch(UnconfinedTestDispatcher(testScheduler)) { needed(settings, habits).toList(answers) }

        CompleteOnboardingUseCase(
            SeedDefaultRoutineUseCase(habits, settings),
            settings,
            HistoryFixtures.clock,
            HistoryFixtures.zone,
        )(RoutineChoice.EMPTY)
        watching.cancel()

        assertEquals(listOf(true, false), answers)
    }

    @Test
    fun saysNothingNewWhenAnUnrelatedSettingChanges() = runTest {
        // Whoever routes on this answer should not be told the same thing twice.
        val settings = FakeSettingsRepository()
        val answers = mutableListOf<Boolean>()
        val watching = launch(UnconfinedTestDispatcher(testScheduler)) {
            needed(settings, FakeHabitRepository()).toList(answers)
        }

        settings.update { it.copy(themeMode = ThemeMode.DARK) }
        watching.cancel()

        assertEquals(listOf(true), answers)
    }
}
