package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.StreakInfo
import dev.ahmad.wird.domain.repository.EntryRepository
import dev.ahmad.wird.domain.repository.HabitRepository
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Observes the user's streak over the last [historyDays] days.
 *
 * The streak rules themselves live in [CalculateStreakUseCase] and are pinned there; this
 * supplies the one thing that can only be got wrong against real data — which days count
 * as complete. A day counts when every habit in force **that** day reached its target, so
 * a day before the routine existed has nothing to complete and cannot bridge a gap.
 *
 * The window is a deliberate limit: a run reaching back further than [historyDays] is
 * measured from the window's edge rather than from its true beginning. Reading all of
 * history to render one number is not worth it, and the number a user reads on a screen is
 * a run they can still see.
 */
class ObserveStreakUseCase(
    private val habits: HabitRepository,
    private val entries: EntryRepository,
    private val calculateDayStats: CalculateDayStatsUseCase,
    private val calculateStreak: CalculateStreakUseCase,
    private val clock: Clock,
    private val zone: TimeZone,
) {
    operator fun invoke(historyDays: Int): Flow<StreakInfo> {
        require(historyDays >= 1) { "a window must cover at least one day, was $historyDays" }

        val today = clock.now().toLocalDateTime(zone).date
        val window = List(historyDays) { offset -> today.plusDays(offset - (historyDays - 1)) }

        return observeDayScores(window, habits, entries, calculateDayStats).map { scores ->
            calculateStreak(
                today = today,
                completeDays = scores.filter { it.stats.isComplete }.map { it.day }.toSet(),
            )
        }
    }
}
