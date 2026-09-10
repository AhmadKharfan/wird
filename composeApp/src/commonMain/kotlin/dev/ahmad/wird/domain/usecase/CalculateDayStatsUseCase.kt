package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.model.DayStats

/**
 * Scores one day.
 *
 * It reads only [DaySnapshot.scheduledHabits] — the habits in force on that snapshot's
 * own day, at the targets they carried then — and never a list taken from today. That is
 * what puts a past day beyond the reach of a later target edit or deactivation.
 *
 * Each habit contributes at most its target. Storage keeps an over-tap so the user can
 * tap past it and wrap back to zero; scoring clamps, or one habit could carry the day
 * past its own maximum.
 */
class CalculateDayStatsUseCase {
    operator fun invoke(snapshot: DaySnapshot): DayStats {
        val maxPoints = snapshot.scheduledHabits.sumOf { it.target }
        val points = snapshot.scheduledHabits.sumOf { habit ->
            minOf(snapshot.valueFor(habit.id), habit.target)
        }

        return DayStats(points = points, maxPoints = maxPoints)
    }
}
