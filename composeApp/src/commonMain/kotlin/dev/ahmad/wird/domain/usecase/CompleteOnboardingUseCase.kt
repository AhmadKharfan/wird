package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.RoutineChoice
import dev.ahmad.wird.domain.repository.SettingsRepository

/**
 * Finishes onboarding with the routine the user chose, and no account.
 *
 * Everything is local: there is no sign-in to wait on, so "use without an account" is not a
 * separate path but the only one there is until accounts exist.
 *
 * The routine is planted **before** onboarding is recorded as finished. Recorded first and
 * then interrupted, the user would land on an empty Today they never chose, with the flag
 * stopping the launch-time seed from ever repairing it; this way round, a failure just means
 * onboarding runs again.
 */
class CompleteOnboardingUseCase(
    private val seedDefaultRoutine: SeedDefaultRoutineUseCase,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(choice: RoutineChoice) {
        if (choice == RoutineChoice.DEFAULT_ROUTINE) seedDefaultRoutine()
        settings.update { it.copy(onboardingCompleted = true) }
    }
}
