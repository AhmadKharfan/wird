package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Observes today as a [DaySnapshot], and moves on to the next day when it begins.
 *
 * The day comes from the clock and zone this instance was given, so "today" follows the user
 * rather than a stored value. Entries keep the day they were written with, so crossing a
 * timezone changes which day is today without dragging yesterday's record along with it.
 *
 * A screen can stay open past midnight, so the day is not resolved once: at each midnight in
 * the zone the observation switches to the new day. Otherwise a tap after midnight would be
 * recorded against yesterday while today looked untouched.
 *
 * Everything else is [ObserveDayUseCase]'s job.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveTodayUseCase(
    private val observeDay: ObserveDayUseCase,
    private val clock: Clock,
    private val zone: TimeZone,
    /**
     * Where the wait for midnight runs: real time unless a test says otherwise. A test
     * scheduler that drains its queue in virtual time would otherwise run a timer that
     * re-arms itself every day for ever; only a test of the rollover itself passes its own
     * dispatcher, to drive the wait in virtual time.
     */
    private val timer: CoroutineDispatcher = Dispatchers.Default,
) {
    operator fun invoke(): Flow<DaySnapshot> = days().flatMapLatest { observeDay(it) }

    /**
     * Today, then each new day as it begins in [zone]. The wait runs to the next day's real
     * start, so a day that is not 24 hours long still turns over at its own midnight.
     */
    private fun days(): Flow<LocalDate> = flow {
        while (true) {
            val now = clock.now()
            val today = now.toLocalDateTime(zone).date
            emit(today)
            // Only the wait moves to the timer. Today is emitted where the collector runs, so
            // the first snapshot arrives the moment it is asked for.
            withContext(timer) { delay(today.plusDays(1).atStartOfDayIn(zone) - now) }
        }
    }.distinctUntilChanged()
}
