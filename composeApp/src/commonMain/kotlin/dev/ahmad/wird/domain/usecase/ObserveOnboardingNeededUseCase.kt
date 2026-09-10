package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.repository.HabitRepository
import dev.ahmad.wird.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Whether this install still has to be set up.
 *
 * Only an install with neither a finished onboarding nor any habit ever is new. The flag
 * alone would send everyone who upgraded from before onboarding existed through it again;
 * the habits alone would do the same to a user who chose to start empty.
 *
 * It follows settings, which is where finishing onboarding is recorded, and says only what
 * changed, so whoever routes on it is not told the same answer twice.
 */
class ObserveOnboardingNeededUseCase(
    private val settings: SettingsRepository,
    private val habits: HabitRepository,
) {
    operator fun invoke(): Flow<Boolean> =
        settings.observeSettings()
            .map { current -> !current.onboardingCompleted && !habits.hasAnyHabit() }
            .distinctUntilChanged()
}
