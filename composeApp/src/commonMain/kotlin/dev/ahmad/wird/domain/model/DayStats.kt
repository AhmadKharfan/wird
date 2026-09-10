package dev.ahmad.wird.domain.model

/**
 * How one day scored.
 *
 * [ratio] and [isComplete] are derived, so the empty-day rule lives here once rather
 * than at every site that builds stats: a day with no habits in force has no maximum,
 * and scores zero without dividing by zero and without counting as finished.
 */
data class DayStats(
    val points: Int,
    val maxPoints: Int,
) {
    init {
        require(points >= 0) { "points must not be negative, was $points" }
        require(maxPoints >= 0) { "maxPoints must not be negative, was $maxPoints" }
        require(points <= maxPoints) {
            "points ($points) must not exceed maxPoints ($maxPoints); scoring clamps each habit to its target"
        }
    }

    /** How much of the day was completed, in `0f..1f`. Zero when there was nothing to do. */
    val ratio: Float get() = if (maxPoints == 0) 0f else points.toFloat() / maxPoints

    /** Whether every point available that day was earned. A day with nothing to do is not complete. */
    val isComplete: Boolean get() = maxPoints > 0 && points >= maxPoints

    companion object {
        /** A day with nothing scheduled and nothing recorded. */
        val EMPTY = DayStats(points = 0, maxPoints = 0)
    }
}
