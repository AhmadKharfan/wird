package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.Entry
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Shared fixtures for the history aggregations. All four read the same shape of data, so
 * building it once keeps the tests about the aggregation rather than about setup.
 */
internal object HistoryFixtures {

    /** Midday on 15 January 2026, so "today" is unambiguous in the zone below. */
    val now: Instant = Instant.parse("2026-01-15T12:00:00Z")
    val zone: TimeZone = UtcOffset.ZERO.asTimeZone()
    val today: LocalDate = LocalDate(2026, 1, 15)

    val clock: Clock = object : Clock {
        override fun now(): Instant = now
    }

    fun habit(
        id: String,
        kind: HabitKind = HabitKind.BOOL,
        target: Int = 1,
        sortOrder: Int = 0,
        effectiveFrom: LocalDate = LocalDate(2020, 1, 1),
        retiredOn: LocalDate? = null,
    ) = Habit(
        id = id,
        name = id,
        kind = kind,
        target = target,
        iconKey = id,
        sortOrder = sortOrder,
        effectiveFrom = effectiveFrom,
        retiredOn = retiredOn,
    )

    fun entry(habitId: String, day: LocalDate, value: Int = 1) = Entry(
        id = "$habitId@$day",
        habitId = habitId,
        day = day,
        value = value,
        updatedAt = now,
    )

    /** Days `[today - (count - 1)] .. today`, oldest first. */
    fun recentDays(count: Int): List<LocalDate> =
        List(count) { offset -> today.plusDays(offset - (count - 1)) }

    /** One completed entry for [habitId] on every day given. */
    fun completedOn(habitId: String, days: List<LocalDate>, value: Int = 1) =
        days.map { entry(habitId, it, value) }
}
