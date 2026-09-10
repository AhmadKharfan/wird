package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.repository.HabitRepository

/**
 * Persists the order the user dragged the habit list into.
 *
 * Order is presentational, so the repository applies it to every revision and a past day
 * renders in the same order as today. The one thing checked here is that the order names
 * each habit once: a duplicate would give a habit two positions and let the later one win
 * silently, so it would not land where the user dropped it.
 */
class ReorderHabitsUseCase(
    private val habits: HabitRepository,
) {
    suspend operator fun invoke(habitIdsInOrder: List<String>) {
        require(habitIdsInOrder.toSet().size == habitIdsInOrder.size) {
            "an order must name each habit once, was $habitIdsInOrder"
        }
        habits.reorder(habitIdsInOrder)
    }
}
