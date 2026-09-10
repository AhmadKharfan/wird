package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * One member's place in their circle's week.
 *
 * It carries a rank and a share of the week, and deliberately no points: the circle ranks by
 * percentage and never by raw points, and a type with no points in it is the one place that
 * rule cannot be broken — no screen can show them, sort by them, or be tempted to.
 */
data class RankedMember(
    val rank: Int,
    val member: CircleMember,
    val isMe: Boolean,
    /** Share of the week so far, in `0f..1f`. */
    val ratio: Float,
)

/**
 * A circle's week, Saturday to Friday, ranked — the circle screen's whole subject.
 *
 * [refreshedAt] is when the underlying copy was fetched, for "last updated"; a leaderboard is
 * always a cached copy, read offline if need be, never a spinner and never an error.
 */
data class Leaderboard(
    val circle: Circle,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val members: List<RankedMember>,
    val groupStreak: Int,
    val refreshedAt: Instant,
)
