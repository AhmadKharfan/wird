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
     * [upsert]s every revision given: all of them, or none. Seeding a routine uses this, so a
     * failure part-way cannot leave half a routine that already counts as "a habit has existed".
     */
    suspend fun upsertAll(revisions: List<Habit>)

    /**
     * Whether any habit has ever existed, active or retired.
     *
     * Asked once at launch to decide whether the default routine still needs seeding. It
     * deliberately counts retired habits: a user who retired the whole routine has made a
     * decision, and an "are there active habits?" check would quietly reverse it.
     */
    suspend fun hasAnyHabit(): Boolean

    /**
     * Every revision of every habit, retired ones included, for the export. The one read
     * not windowed to the days in force: a backup has to hold history the screens no
     * longer show.
     */
    suspend fun allRevisions(): List<Habit>

    /**
     * Renumbers [Habit.sortOrder] to match the given order. Reordering is presentational,
     * so it applies to every revision of each habit rather than opening a new one.
     */
    suspend fun reorder(habitIdsInOrder: List<String>)

    /**
     * Retires the habit from [asOf] onward, or reinstates it from [asOf] onward.
     *
     * [asOf] is exclusive when retiring: passing today removes the habit from *today's*
     * maximum and leaves every earlier day counting it. Reinstating never reaches back: the
     * days the habit was off stay off, and it returns with the target it left with.
     */
    suspend fun setActive(habitId: String, active: Boolean, asOf: LocalDate)

    /**
     * Renames the habit and changes its icon on **every** revision.
     *
     * Name and icon are presentational, like order: a rename should read the same on every
     * day the habit appears. That is the opposite of a target change, which [upsert] applies
     * from a day onward and never backwards — so appearance is its own operation, and it
     * never opens a revision or touches anything that scores.
     */
    suspend fun updateAppearance(habitId: String, name: String, iconKey: String)
}
