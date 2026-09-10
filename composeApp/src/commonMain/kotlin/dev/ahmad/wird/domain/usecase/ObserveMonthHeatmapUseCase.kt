package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DayScore
import dev.ahmad.wird.domain.repository.EntryRepository
import dev.ahmad.wird.domain.repository.HabitRepository
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Observes every day of one calendar month with its score.
 *
 * It emits the month's days and nothing else — no grid, no leading blanks for the days
 * before the first. Weeks start on Saturday, so arranging these into rows is a layout
 * decision, and layout decisions do not belong in the domain.
 */
class ObserveMonthHeatmapUseCase(
    private val habits: HabitRepository,
    private val entries: EntryRepository,
    private val calculateDayStats: CalculateDayStatsUseCase,
) {
    operator fun invoke(anyDayInMonth: LocalDate): Flow<List<DayScore>> =
        observeDayScores(daysOfMonth(anyDayInMonth), habits, entries, calculateDayStats)

    /**
     * Walks forward from the first of the month until the month changes, so month length
     * and leap years are the calendar's answer rather than a table here.
     */
    private fun daysOfMonth(anyDayInMonth: LocalDate): List<LocalDate> {
        val first = LocalDate(anyDayInMonth.year, anyDayInMonth.month, 1)
        return generateSequence(first) { it.plusDays(1) }
            .takeWhile { it.month == first.month }
            .toList()
    }
}
