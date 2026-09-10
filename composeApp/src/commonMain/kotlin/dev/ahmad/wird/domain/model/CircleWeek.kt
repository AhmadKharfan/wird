package dev.ahmad.wird.domain.model

import dev.ahmad.wird.domain.util.WeekBoundary
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * One member's week so far, as their circle sees it.
 *
 * Two totals and nothing else. There is deliberately no field that could say which habit a
 * member did or missed: the product shows totals only, and a type with no such field is the
 * one place that rule cannot be broken by a screen.
 */
data class MemberWeek(
    val member: CircleMember,
    val isMe: Boolean,
    val points: Int,
    val maxPoints: Int,
) {
    init {
        require(points >= 0) { "points must not be negative, was $points" }
        require(maxPoints >= 0) { "maxPoints must not be negative, was $maxPoints" }
        require(points <= maxPoints) { "points ($points) must not exceed maxPoints ($maxPoints)" }
    }

    /** Share of the week earned, in `0f..1f`. Zero before anything was scheduled. */
    val ratio: Float get() = if (maxPoints == 0) 0f else points.toFloat() / maxPoints
}

/**
 * A circle's week, Saturday to Friday, as last fetched.
 *
 * [refreshedAt] is when this copy was fetched, so a screen can show it offline as "last
 * updated" instead of a spinner or an error. [groupStreak] comes from the backend, the only
 * party that sees every member's days — this client only ever sees their totals.
 */
data class CircleWeek(
    val circle: Circle,
    val weekStart: LocalDate,
    val members: List<MemberWeek>,
    val groupStreak: Int,
    val refreshedAt: Instant,
) {
    init {
        require(WeekBoundary.startOfWeek(weekStart) == weekStart) {
            "a circle week starts on Saturday, was $weekStart"
        }
        require(members.count { it.isMe } <= 1) { "at most one member of a circle is the user" }
        require(groupStreak >= 0) { "group streak must not be negative, was $groupStreak" }
    }
}
