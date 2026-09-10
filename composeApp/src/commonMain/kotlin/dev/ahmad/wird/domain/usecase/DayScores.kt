package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DayScore
import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.repository.EntryRepository
import dev.ahmad.wird.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * Snapshots a run of days from two queries rather than two per day.
 *
 * Habit revisions and entries are both read once for the whole span, then a snapshot is
 * built per day from the same two lists. That works because a snapshot already narrows the
 * revisions it is handed down to the ones live on its own day, so each day still scores
 * against its own history — the shared read is an efficiency, not a shortcut past the rule.
 *
 * Every period view goes through here, so none of them can drift from another.
 */
internal fun observeSnapshots(
    days: List<LocalDate>,
    habits: HabitRepository,
    entries: EntryRepository,
): Flow<List<DaySnapshot>> {
    require(days.isNotEmpty()) { "cannot snapshot an empty run of days" }

    return combine(
        habits.observeHabitsIn(days.first(), days.last()),
        entries.observeRange(days.first(), days.last()),
    ) { revisions, recorded ->
        val entriesByDay = recorded.groupBy { it.day }
        days.map { day ->
            DaySnapshot(
                day = day,
                habits = revisions,
                entries = entriesByDay[day].orEmpty(),
            )
        }
    }
}

/** The same run of days, scored. */
internal fun observeDayScores(
    days: List<LocalDate>,
    habits: HabitRepository,
    entries: EntryRepository,
    calculateDayStats: CalculateDayStatsUseCase,
): Flow<List<DayScore>> =
    observeSnapshots(days, habits, entries).map { snapshots ->
        snapshots.map { DayScore(day = it.day, stats = calculateDayStats(it)) }
    }
