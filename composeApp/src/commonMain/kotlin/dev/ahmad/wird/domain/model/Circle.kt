package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate

/** A small group that sees each other's daily ratio. */
data class Circle(
    val id: String,
    val name: String,
    val inviteCode: String,
    val memberCount: Int,
)

/**
 * One person in a circle. [displayName] is null when they joined under
 * [PrivacyMode.ANONYMOUS], so anonymity is represented rather than approximated by a
 * placeholder name.
 */
data class CircleMember(
    val id: String,
    val displayName: String?,
)

/** How one member scored on one day, as their circle sees it. */
data class CircleStanding(
    val member: CircleMember,
    val day: LocalDate,
    val stats: DayStats,
)
