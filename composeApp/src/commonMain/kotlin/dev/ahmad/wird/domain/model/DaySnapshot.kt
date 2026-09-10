package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate

/**
 * One day as it actually stood: the habits in force on [day], and what was recorded
 * against them.
 *
 * This is the unit of scoring. Everything that computes points reads [scheduledHabits],
 * never a list of habits taken from today, which is what stops a target edit or a
 * deactivation from rescoring days that are already past. [habits] may therefore contain
 * revisions that do not apply here; they are filtered out rather than rejected.
 */
data class DaySnapshot(
    val day: LocalDate,
    val habits: List<Habit>,
    val entries: List<Entry>,
) {
    /** The habits in force on [day], in display order. */
    val scheduledHabits: List<Habit> =
        habits.filter { it.isLiveOn(day) }.sortedBy { it.sortOrder }

    private val valuesByHabitId: Map<String, Int> = entries.associate { it.habitId to it.value }

    init {
        require(entries.all { it.day == day }) {
            "every entry in a snapshot of $day must belong to that day"
        }
        require(valuesByHabitId.size == entries.size) {
            "a habit may have at most one entry per day"
        }
        require(scheduledHabits.distinctBy { it.id }.size == scheduledHabits.size) {
            "two revisions of the same habit are live on $day; their windows overlap"
        }
    }

    /** What was recorded for [habitId], or zero when nothing was. Never null. */
    fun valueFor(habitId: String): Int = valuesByHabitId[habitId] ?: 0
}
