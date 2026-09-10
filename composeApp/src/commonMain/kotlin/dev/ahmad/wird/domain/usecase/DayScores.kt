package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DayScore
import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.repository.EntryRepository
import dev.ahmad.wird.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate

/**
 * Scores a run of days from two queries rather than two per day.
 *
 * Habit revisions and entries are both read once for the whole span, then a snapshot is
 * built per day from the same two lists. That works because a snapshot already narrows
 * the revisions it is handed down to the ones live on its own day, so each day still
 * scores against its own history — the shared read is an efficiency, not a shortcut past
 * the rule.
 *
 * Shared by the week and month views so they cannot drift apart.
 */
internal fun observeDayScores(
    days: List<LocalDate>,
    habits: HabitRepository,
    entries: EntryRepository,
    calculateDayStats: CalculateDayStatsUseCase,
): Flow<List<DayScore>> {
    require(days.isNotEmpty()) { "cannot score an empty run of days" }

    return combine(
        habits.observeHabitsIn(days.first(), days.last()),
        entries.observeRange(days.first(), days.last()),
    ) { revisions, recorded ->
        days.map { day ->
            val snapshot = DaySnapshot(
                day = day,
                habits = revisions,
                entries = recorded.filter { it.day == day },
            )
            DayScore(day = day, stats = calculateDayStats(snapshot))
        }
    }
}
