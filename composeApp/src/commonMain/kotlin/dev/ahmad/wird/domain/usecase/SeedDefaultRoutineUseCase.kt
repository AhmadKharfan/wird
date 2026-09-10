package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DefaultRoutine
import dev.ahmad.wird.domain.repository.HabitRepository
import dev.ahmad.wird.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Plants the default routine on a fresh install, starting today.
 *
 * Called on every launch, so it has to be a no-op on all but the first. "Fresh" means no
 * habit has ever existed, not that none is currently active — a user who retired the
 * whole routine has made a decision, and finding it back the next morning would undo it.
 *
 * A finished onboarding is a decision too. A user who chose to start empty has no habit at
 * all, so without that check they would be handed the routine they declined.
 *
 * Which day is today is decided here, from the clock and zone, rather than by whoever calls:
 * it is a date rule, and date rules live in the domain.
 */
class SeedDefaultRoutineUseCase(
    private val habits: HabitRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {
    suspend operator fun invoke() {
        if (settings.observeSettings().first().onboardingCompleted) return
        if (habits.hasAnyHabit()) return

        // In one write: a seed that stopped part way would already count as "a habit has
        // existed", and the rest of the routine would never be planted.
        habits.upsertAll(DefaultRoutine.habitsFrom(clock.now().toLocalDateTime(zone).date))
    }
}
