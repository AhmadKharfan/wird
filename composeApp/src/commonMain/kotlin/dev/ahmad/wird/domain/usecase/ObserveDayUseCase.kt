package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.repository.EntryRepository
import dev.ahmad.wird.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate

/**
 * Observes one named day as a [DaySnapshot] — the habits in force **that** day, and what
 * was recorded against them.
 *
 * The day is a parameter rather than a clock reading, which is what lets the day-detail
 * sheet open a day in the past and still score it against the routine as it stood then.
 * [ObserveTodayUseCase] is this with the day resolved from the clock.
 */
class ObserveDayUseCase(
    private val habits: HabitRepository,
    private val entries: EntryRepository,
) {
    operator fun invoke(day: LocalDate): Flow<DaySnapshot> =
        combine(
            habits.observeHabitsOn(day),
            entries.observeDay(day),
        ) { liveHabits, recorded ->
            DaySnapshot(day = day, habits = liveHabits, entries = recorded)
        }
}
