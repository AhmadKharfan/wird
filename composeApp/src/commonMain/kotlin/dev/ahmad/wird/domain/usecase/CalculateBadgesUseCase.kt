package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.Badge
import dev.ahmad.wird.domain.model.BadgeKind
import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.model.DefaultRoutine
import dev.ahmad.wird.domain.util.WeekBoundary
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.datetime.LocalDate

/**
 * Works out every badge from local history. Pure: history in, badges out.
 *
 * Every badge is permanent and dated to the day it was earned, so each is found by walking
 * history forward and stopping at the first day that qualifies; nothing later can undo it.
 *
 * - **Streaks** count consecutive *calendar* days that were complete, so a gap in history
 *   breaks a run exactly as a missed day does. A day with nothing scheduled is not complete,
 *   so the days before a routine existed cannot bridge anything.
 * - **Perfect weeks and five-prayer weeks** are Saturday-to-Friday weeks with all seven days
 *   present and kept, dated to their Friday. Seven good days that straddle a Saturday are a
 *   streak, not a perfect week, and a week still in progress is not whole yet.
 * - **Consistency** slides a thirty-day window over each habit's life. The habit has to have
 *   been live on every one of the thirty days — ten perfect days are not thirty days of
 *   consistency — and kept on at least 90% of them. Each day is judged against the target the
 *   habit had that day, so a later target change does not rewrite an earned badge.
 * - **Five prayers** follows the default routine's five-prayers habit by id, and needs only
 *   that habit kept, not a complete day.
 */
class CalculateBadgesUseCase(
    private val calculateDayStats: CalculateDayStatsUseCase = CalculateDayStatsUseCase(),
) {
    operator fun invoke(history: List<DaySnapshot>): List<Badge> {
        val days = history.sortedBy { it.day }

        return buildList {
            STREAK_MILESTONES.forEach { (kind, length) ->
                add(Badge(kind, earnedOn = firstDayRunReaches(days, length)))
            }
            add(Badge(BadgeKind.PERFECT_WEEK, earnedOn = firstWholeWeek(days) { calculateDayStats(it).isComplete }))
            add(Badge(BadgeKind.FIVE_PRAYERS_WEEK, earnedOn = firstWholeWeek(days) { it.keptFivePrayers() }))
            habitIdsIn(days).forEach { habitId ->
                add(Badge(BadgeKind.HABIT_CONSISTENCY, earnedOn = firstConsistentWindow(days, habitId), habitId = habitId))
            }
        }
    }

    private fun firstDayRunReaches(days: List<DaySnapshot>, length: Int): LocalDate? {
        var run = 0
        var previous: LocalDate? = null
        for (snapshot in days) {
            val continues = run > 0 && previous?.plusDays(1) == snapshot.day
            run = when {
                !calculateDayStats(snapshot).isComplete -> 0
                continues -> run + 1
                else -> 1
            }
            if (run == length) return snapshot.day
            previous = snapshot.day
        }
        return null
    }

    private fun firstWholeWeek(days: List<DaySnapshot>, kept: (DaySnapshot) -> Boolean): LocalDate? =
        days.groupBy { WeekBoundary.startOfWeek(it.day) }
            .entries.sortedBy { it.key }
            .firstOrNull { (_, week) -> week.size == DAYS_IN_WEEK && week.all(kept) }
            ?.let { (start, _) -> WeekBoundary.endOfWeek(start) }

    private fun firstConsistentWindow(days: List<DaySnapshot>, habitId: String): LocalDate? {
        val window = ArrayDeque<Boolean>()
        var previous: LocalDate? = null
        for (snapshot in days) {
            // A gap in history is days we know nothing about, so no window may span it.
            if (previous != null && previous.plusDays(1) != snapshot.day) window.clear()
            previous = snapshot.day

            val habit = snapshot.scheduledHabits.firstOrNull { it.id == habitId }
            if (habit == null) {
                // Not live today, so no window containing today is a full stretch of its life.
                window.clear()
                continue
            }
            window.addLast(snapshot.valueFor(habitId) >= habit.target)
            if (window.size > CONSISTENCY_WINDOW) window.removeFirst()

            val kept = window.count { it }
            if (window.size == CONSISTENCY_WINDOW && kept * 10 >= CONSISTENCY_WINDOW * 9) return snapshot.day
        }
        return null
    }

    private fun habitIdsIn(days: List<DaySnapshot>): List<String> =
        days.flatMap { it.scheduledHabits }
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .map { it.id }

    private fun DaySnapshot.keptFivePrayers(): Boolean {
        val prayers = scheduledHabits.firstOrNull { it.id == DefaultRoutine.FIVE_PRAYERS_ID } ?: return false
        return valueFor(prayers.id) >= prayers.target
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
        const val CONSISTENCY_WINDOW = 30

        val STREAK_MILESTONES = listOf(
            BadgeKind.STREAK_7 to 7,
            BadgeKind.STREAK_30 to 30,
            BadgeKind.STREAK_100 to 100,
        )
    }
}
