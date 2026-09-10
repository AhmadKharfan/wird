package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DaySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Observes today as a [DaySnapshot].
 *
 * The day is resolved when the flow is asked for, from the clock and zone this instance was
 * given, so "today" follows the user rather than a stored value. Entries keep the day they
 * were written with, so crossing a timezone changes which day is today without dragging
 * yesterday's record along with it.
 *
 * Everything after that is [ObserveDayUseCase]'s job; today is simply the day the clock
 * names.
 */
class ObserveTodayUseCase(
    private val observeDay: ObserveDayUseCase,
    private val clock: Clock,
    private val zone: TimeZone,
) {
    operator fun invoke(): Flow<DaySnapshot> =
        observeDay(clock.now().toLocalDateTime(zone).date)
}
