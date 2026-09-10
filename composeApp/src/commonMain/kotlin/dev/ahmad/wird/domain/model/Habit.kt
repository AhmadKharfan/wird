package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate

/**
 * One habit the user has committed to, over the window it applies to.
 *
 * A habit is **effective-dated**: [effectiveFrom] is inclusive, [retiredOn] is exclusive.
 * Editing a target does not mutate the row in place — it retires the current revision and
 * opens a new one from today, so a past day keeps scoring against the target that was
 * actually in force then. Deactivating sets [retiredOn] to today, which drops the habit
 * from today's maximum while every earlier day still counts it.
 *
 * [id] is the stable identity shared by every revision of the same habit; two revisions
 * differ by their window, not by their id.
 */
data class Habit(
    val id: String,
    val name: String,
    val kind: HabitKind,
    val target: Int,
    val iconKey: String,
    val sortOrder: Int,
    val effectiveFrom: LocalDate,
    val retiredOn: LocalDate? = null,
) {
    init {
        require(id.isNotBlank()) { "habit id must not be blank" }
        require(name.isNotBlank()) { "habit name must not be blank" }
        require(target >= 1) { "habit target must be at least 1, was $target" }
        require(kind != HabitKind.BOOL || target == 1) {
            "a BOOL habit is worth one point, so its target must be 1, was $target"
        }
        require(sortOrder >= 0) { "habit sortOrder must not be negative, was $sortOrder" }
        require(retiredOn == null || retiredOn >= effectiveFrom) {
            "habit retiredOn ($retiredOn) must not precede effectiveFrom ($effectiveFrom)"
        }
    }

    /** Whether the habit is still in force today. Derived, so it cannot contradict [retiredOn]. */
    val active: Boolean get() = retiredOn == null

    /** Whether this revision applies on [day], and therefore counts toward that day's maximum. */
    fun isLiveOn(day: LocalDate): Boolean =
        day >= effectiveFrom && (retiredOn == null || day < retiredOn)
}
