package dev.ahmad.wird.domain.model

/**
 * What the habit edit sheet hands back: the user's intent, before it is a [Habit].
 *
 * A draft is not a revision. It carries no dates and no position — a null [id] means a new
 * habit, and deciding when a change takes effect and where a habit sits in the list is the
 * save use case's job rather than the sheet's. That is what keeps the sheet structurally
 * unable to backdate an edit.
 */
data class HabitDraft(
    val id: String?,
    val name: String,
    val kind: HabitKind,
    val target: Int,
    val iconKey: String,
)
