package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate

/** The achievements the app recognises, all computed from local history. */
enum class BadgeKind {
    /** A run of seven complete days. */
    STREAK_7,

    /** A run of thirty complete days. */
    STREAK_30,

    /** A run of a hundred complete days. */
    STREAK_100,

    /** Every day of a Saturday-to-Friday week complete. */
    PERFECT_WEEK,

    /** One habit kept on at least 90% of a full thirty days of its life. */
    HABIT_CONSISTENCY,

    /** The five prayers kept every day of a Saturday-to-Friday week. */
    FIVE_PRAYERS_WEEK,
}

/**
 * One badge and whether it has been earned.
 *
 * Badges are permanent: once [earnedOn] is set, a later bad stretch does not take it back.
 * A locked badge has no date. Only a consistency badge belongs to a habit, so only it
 * carries a [habitId].
 */
data class Badge(
    val kind: BadgeKind,
    val earnedOn: LocalDate?,
    val habitId: String? = null,
) {
    init {
        require((kind == BadgeKind.HABIT_CONSISTENCY) == (habitId != null)) {
            "only a consistency badge belongs to a habit; $kind had habitId $habitId"
        }
    }

    val earned: Boolean get() = earnedOn != null
}
