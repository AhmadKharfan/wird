package dev.ahmad.wird.domain.util

import kotlinx.datetime.LocalDate

/**
 * Day arithmetic over the epoch-day count, so month lengths, year ends and leap days are
 * the calendar's problem rather than the caller's.
 */
internal fun LocalDate.plusDays(days: Int): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() + days)

internal fun LocalDate.minusDays(days: Int): LocalDate = plusDays(-days)
