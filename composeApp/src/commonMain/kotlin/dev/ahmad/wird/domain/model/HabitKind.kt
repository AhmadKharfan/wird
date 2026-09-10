package dev.ahmad.wird.domain.model

/** How a habit is scored: done-or-not, or counted up to a target. */
enum class HabitKind {
    /** Worth exactly one point. Its target is always 1. */
    BOOL,

    /** Worth up to [Habit.target] points, one per completed repetition. */
    COUNTER,
}
