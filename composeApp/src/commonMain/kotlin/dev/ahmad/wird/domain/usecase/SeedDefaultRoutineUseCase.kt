package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DefaultRoutine
import dev.ahmad.wird.domain.repository.HabitRepository
import kotlinx.datetime.LocalDate

/**
 * Plants the default routine on a fresh install.
 *
 * Called on every launch, so it has to be a no-op on all but the first. "Fresh" means no
 * habit has ever existed, not that none is currently active — a user who retired the
 * whole routine has made a decision, and finding it back the next morning would undo it.
 */
class SeedDefaultRoutineUseCase(
    private val habits: HabitRepository,
) {
    suspend operator fun invoke(today: LocalDate) {
        if (habits.hasAnyHabit()) return

        DefaultRoutine.habitsFrom(today).forEach { habits.upsert(it) }
    }
}
