package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

/**
 * The habits in force today, in display order — what the reorderable habit list shows.
 *
 * A pass-through, and kept as one because ViewModels call use cases only, never a
 * repository.
 */
class ObserveActiveHabitsUseCase(
    private val habits: HabitRepository,
) {
    operator fun invoke(): Flow<List<Habit>> = habits.observeActiveHabits()
}
