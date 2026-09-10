package dev.ahmad.wird.domain.repository

import dev.ahmad.wird.domain.model.Entry
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Local-first: every write completes against local storage and returns.
 *
 * Days here are the user's local calendar days as recorded. Implementations store the
 * day they are given and never re-derive it from a clock or a timezone, so moving
 * between zones cannot shift an entry onto a neighbouring day.
 */
interface EntryRepository {

    /** Everything recorded on [day]. Emits an empty list for a day with nothing on it. */
    fun observeDay(day: LocalDate): Flow<List<Entry>>

    /** Everything recorded between [from] and [to], both inclusive. */
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Entry>>

    /**
     * Records [value] against the habit on that day, replacing whatever was there.
     * A value of zero is stored rather than deleted, so "explicitly not done" stays
     * distinguishable from "never opened".
     */
    suspend fun setValue(habitId: String, day: LocalDate, value: Int)

    /**
     * Flips a stored value between zero and one atomically: zero becomes one, anything
     * else becomes zero.
     *
     * This is a storage-level flip and knows nothing about targets. Deciding what a tap
     * means for a counter is scoring, and belongs to the use case, not here.
     */
    suspend fun toggle(habitId: String, day: LocalDate)
}
