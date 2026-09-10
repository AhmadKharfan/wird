package dev.ahmad.wird.domain.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/**
 * Weeks start on Saturday.
 *
 * This is a product decision, not a locale one: the app is Arabic and its week runs
 * Saturday to Friday regardless of the host's calendar settings, exactly as the layout
 * direction is forced rather than inherited. Every week-shaped view resolves its
 * boundaries through here so none of them can disagree.
 */
object WeekBoundary {

    private const val DAYS_IN_WEEK = 7

    /**
     * How far back the containing Saturday is.
     *
     * ISO numbers Monday 1 through Sunday 7, so Saturday is 6. Adding one and taking the
     * remainder rotates that scale to put Saturday at zero: Saturday 0, Sunday 1, and on
     * to Friday 6.
     */
    private val LocalDate.daysSinceWeekStart: Int
        get() = (dayOfWeek.isoDayNumber + 1) % DAYS_IN_WEEK

    /** The Saturday that opens the week containing [day]. A Saturday returns itself. */
    fun startOfWeek(day: LocalDate): LocalDate = day.minusDays(day.daysSinceWeekStart)

    /** The Friday that closes the week containing [day]. */
    fun endOfWeek(day: LocalDate): LocalDate = startOfWeek(day).plusDays(DAYS_IN_WEEK - 1)

    /** The seven days of the week containing [day], Saturday first. */
    fun daysOfWeek(day: LocalDate): List<LocalDate> {
        val start = startOfWeek(day)
        return List(DAYS_IN_WEEK) { offset -> start.plusDays(offset) }
    }
}
