package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.WeekSummary
import dev.ahmad.wird.domain.repository.EntryRepository
import dev.ahmad.wird.domain.repository.HabitRepository
import dev.ahmad.wird.domain.util.WeekBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * Observes the week containing a given day, Saturday to Friday.
 *
 * The caller passes any day of the week rather than its start, so no screen has to know
 * where a week begins — that answer lives in [WeekBoundary] alone.
 */
class ObserveWeekSummaryUseCase(
    private val habits: HabitRepository,
    private val entries: EntryRepository,
    private val calculateDayStats: CalculateDayStatsUseCase,
) {
    operator fun invoke(anyDayInWeek: LocalDate): Flow<WeekSummary> {
        val days = WeekBoundary.daysOfWeek(anyDayInWeek)

        return observeDayScores(days, habits, entries, calculateDayStats)
            .map { scores -> WeekSummary(startDay = days.first(), days = scores) }
    }
}
