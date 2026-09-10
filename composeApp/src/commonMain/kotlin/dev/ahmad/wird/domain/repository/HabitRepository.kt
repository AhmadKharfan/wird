package dev.ahmad.wird.domain.repository

import dev.ahmad.wird.domain.model.Habit
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Local-first: every write completes against local storage and returns. Nothing here
 * returns a network result, and no implementation may await one.
 *
 * Habits are stored as effective-dated revisions sharing one [Habit.id]. That is why
 * reading habits for a past day is a different question from reading today's, and why
 * both writes below are expressed as "from this day onward" rather than as edits in place.
 */
interface HabitRepository {

    /** The habits in force today, in display order. */
    fun observeActiveHabits(): Flow<List<Habit>>

    /**
     * The habits in force on [day] — the revisions that were live then, at the targets
     * they carried then. This is what makes a past [dev.ahmad.wird.domain.model.DaySnapshot]
     * score against history rather than against today's routine.
     */
    fun observeHabitsOn(day: LocalDate): Flow<List<Habit>>

    /**
     * Every revision live on at least one day between [from] and [to], both inclusive.
     *
     * The range form exists so a week or a month is one query rather than one per day.
     * Callers hand the whole list to a snapshot per day and let
     * [dev.ahmad.wird.domain.model.DaySnapshot.scheduledHabits] pick out the revision that
     * applies to each; [observeHabitsOn] is the single-day case the Today screen uses.
     */
    fun observeHabitsIn(from: LocalDate, to: LocalDate): Flow<List<Habit>>

    /**
     * Creates a habit, or revises one that already exists.
     *
     * Revising never edits a past row. The implementation closes the current revision at
     * [Habit.effectiveFrom] and opens the given one from there, so every day before it
     * keeps the target that was actually in force. Passing an [Habit.effectiveFrom]
     * earlier than the current revision's start is a programming error.
     */
    suspend fun upsert(habit: Habit)

    /**
     * Renumbers [Habit.sortOrder] to match the given order. Reordering is presentational,
     * so it applies to every revision of each habit rather than opening a new one.
     */
    suspend fun reorder(habitIdsInOrder: List<String>)

    /**
     * Retires the habit from [asOf] onward, or reinstates it from [asOf] onward.
     *
     * [asOf] is exclusive when retiring: passing today removes the habit from *today's*
     * maximum and leaves every earlier day counting it.
     */
    suspend fun setActive(habitId: String, active: Boolean, asOf: LocalDate)
}
