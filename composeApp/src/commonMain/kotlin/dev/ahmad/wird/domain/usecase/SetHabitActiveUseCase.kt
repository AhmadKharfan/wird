package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.repository.HabitRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Deactivates a habit, or brings one back, as of today.
 *
 * Deliberately thin. The rule that matters — deactivating removes a habit from today's
 * maximum while every earlier day keeps counting it — was settled in the data layer, where
 * retirement is exclusive of the day it happens. This supplies "today" and nothing else, so
 * there is exactly one place that rule lives.
 */
class SetHabitActiveUseCase(
    private val habits: HabitRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {
    suspend operator fun invoke(habitId: String, active: Boolean) {
        habits.setActive(habitId, active, asOf = clock.now().toLocalDateTime(zone).date)
    }
}
