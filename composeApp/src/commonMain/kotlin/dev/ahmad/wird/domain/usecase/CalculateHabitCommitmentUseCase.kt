package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitCommitment

/**
 * Keeps commitment fair by judging a habit only on days when it was actually in force.
 */
class CalculateHabitCommitmentUseCase {
    operator fun invoke(habit: Habit, snapshots: List<DaySnapshot>): HabitCommitment {
        val liveSnapshots = snapshots.filter { habit.isLiveOn(it.day) }
        val completedDays = liveSnapshots.count { snapshot -> habit.isKeptBy(snapshot.valueFor(habit.id)) }

        return HabitCommitment(
            habit = habit,
            completedDays = completedDays,
            elapsedDays = liveSnapshots.size,
        )
    }
}
