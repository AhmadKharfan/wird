package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate

/**
 * The run of complete days ending now, and the best run on record.
 *
 * [current] is zero once a run breaks, but [longest] and [lastCompleteDay] survive it.
 * Today counts toward [current] only once it is complete; an unfinished today leaves the
 * run measured to yesterday rather than breaking it.
 */
data class StreakInfo(
    val current: Int,
    val longest: Int,
    val lastCompleteDay: LocalDate?,
) {
    init {
        require(current >= 0) { "current streak must not be negative, was $current" }
        require(longest >= current) {
            "longest streak ($longest) must be at least the current one ($current)"
        }
        require((longest > 0) == (lastCompleteDay != null)) {
            "a streak of $longest and a last complete day of $lastCompleteDay disagree about whether any day was ever completed"
        }
    }

    companion object {
        /** No day has ever been completed. */
        val NONE = StreakInfo(current = 0, longest = 0, lastCompleteDay = null)
    }
}
