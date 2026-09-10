package dev.ahmad.wird.domain.model

/**
 * How faithfully one habit has been kept over the days it was actually in force.
 *
 * [elapsedDays] counts only days the habit was live, so a habit added last week is not
 * judged against the months before it existed, and a retired habit stops accruing misses.
 */
data class HabitCommitment(
    val habit: Habit,
    val completedDays: Int,
    val elapsedDays: Int,
) {
    init {
        require(completedDays >= 0) { "completedDays must not be negative, was $completedDays" }
        require(elapsedDays >= 0) { "elapsedDays must not be negative, was $elapsedDays" }
        require(completedDays <= elapsedDays) {
            "completedDays ($completedDays) must not exceed elapsedDays ($elapsedDays)"
        }
    }

    /** Share of live days the habit was completed on, in `0f..1f`. Zero before any day has elapsed. */
    val ratio: Float get() = if (elapsedDays == 0) 0f else completedDays.toFloat() / elapsedDays
}
