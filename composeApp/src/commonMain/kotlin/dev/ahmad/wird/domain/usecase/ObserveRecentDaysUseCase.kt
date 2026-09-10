package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DayScore
import dev.ahmad.wird.domain.repository.EntryRepository
import dev.ahmad.wird.domain.repository.HabitRepository
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Observes the last [days] days, ending today, scored.
 *
 * Always **oldest first**. The chart that draws this decides which way its own time axis
 * runs; having the data carry a second, silent direction decision is how the bar chart and
 * the calendar end up disagreeing with each other.
 */
class ObserveRecentDaysUseCase(
    private val habits: HabitRepository,
    private val entries: EntryRepository,
    private val calculateDayStats: CalculateDayStatsUseCase,
    private val clock: Clock,
    private val zone: TimeZone,
) {
    operator fun invoke(days: Int): Flow<List<DayScore>> {
        require(days >= 1) { "a window must cover at least one day, was $days" }

        val today = clock.now().toLocalDateTime(zone).date
        val window = List(days) { offset -> today.plusDays(offset - (days - 1)) }

        return observeDayScores(window, habits, entries, calculateDayStats)
    }
}
