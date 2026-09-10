package dev.ahmad.wird.domain.model

import dev.ahmad.wird.domain.util.plusDays
import kotlinx.datetime.LocalDate

/** One day and how it scored. The unit every period-shaped view is built from. */
data class DayScore(
    val day: LocalDate,
    val stats: DayStats,
)

/**
 * One week, Saturday to Friday, with each of its seven days scored.
 *
 * The days are required to be seven consecutive days starting at [startDay]. A gap would
 * silently shrink [maxPoints] and flatter every ratio derived from it, so it is rejected
 * outright rather than tolerated.
 */
data class WeekSummary(
    val startDay: LocalDate,
    val days: List<DayScore>,
) {
    init {
        require(days.size == DAYS_IN_WEEK) {
            "a week has $DAYS_IN_WEEK days, was given ${days.size}"
        }
        require(days.withIndex().all { (offset, score) -> score.day == startDay.plusDays(offset) }) {
            "the days of the week beginning $startDay must be consecutive and start at it"
        }
    }

    /** The Friday that closes the week. */
    val endDay: LocalDate get() = startDay.plusDays(DAYS_IN_WEEK - 1)

    /** Points earned across the whole week. */
    val points: Int get() = days.sumOf { it.stats.points }

    /** Points that were available across the whole week. */
    val maxPoints: Int get() = days.sumOf { it.stats.maxPoints }

    /** How much of the week was completed, in `0f..1f`. Zero when there was nothing to do. */
    val ratio: Float get() = if (maxPoints == 0) 0f else points.toFloat() / maxPoints

    /** How many of the seven days were finished outright. */
    val completeDays: Int get() = days.count { it.stats.isComplete }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}
