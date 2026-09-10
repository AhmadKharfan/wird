package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.repository.EntryRepository
import dev.ahmad.wird.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Observes today as a [DaySnapshot] — the habits in force today, and what has been
 * recorded against them.
 *
 * The day is resolved when the flow is asked for, from the clock and zone this instance
 * was given, so "today" follows the user rather than a stored value. Entries keep the day
 * they were written with, so crossing a timezone changes which day is today without
 * dragging yesterday's record along with it.
 */
class ObserveTodayUseCase(
    private val habits: HabitRepository,
    private val entries: EntryRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {
    operator fun invoke(): Flow<DaySnapshot> {
        val today = clock.now().toLocalDateTime(zone).date
        return combine(
            habits.observeHabitsOn(today),
            entries.observeDay(today),
        ) { liveHabits, recorded ->
            DaySnapshot(day = today, habits = liveHabits, entries = recorded)
        }
    }
}
