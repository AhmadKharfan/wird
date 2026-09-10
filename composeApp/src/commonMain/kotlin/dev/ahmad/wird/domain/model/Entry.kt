package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * What the user recorded against one habit on one day.
 *
 * [day] is the user's local calendar day at the moment of recording, and it is
 * authoritative: nothing re-derives it from [updatedAt], so moving between timezones
 * never shifts an entry onto a neighbouring day. [updatedAt] is write metadata only,
 * kept for last-write-wins once syncing lands.
 *
 * For a [HabitKind.BOOL] habit [value] is 0 or 1; for a [HabitKind.COUNTER] it is the
 * number of repetitions done, which may exceed the target — scoring clamps, storage
 * does not.
 */
data class Entry(
    val id: String,
    val habitId: String,
    val day: LocalDate,
    val value: Int,
    val updatedAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "entry id must not be blank" }
        require(habitId.isNotBlank()) { "entry habitId must not be blank" }
        require(value >= 0) { "entry value must not be negative, was $value" }
    }
}
