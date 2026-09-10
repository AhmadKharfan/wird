package dev.ahmad.wird.domain.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Clock
import kotlin.time.Instant

/** Pure helpers over dates. No I/O, no platform types. */
object DayBoundaries {

    fun startOf(date: LocalDate, zone: TimeZone): Instant = date.atStartOfDayIn(zone)

    fun isToday(date: LocalDate, zone: TimeZone, clock: Clock): Boolean =
        clock.now() >= startOf(date, zone) && clock.now() < startOf(date.plusDays(1), zone)

    private fun LocalDate.plusDays(days: Int): LocalDate =
        LocalDate.fromEpochDays(toEpochDays() + days)
}
