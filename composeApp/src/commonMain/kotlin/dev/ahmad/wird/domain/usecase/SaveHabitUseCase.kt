package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitDraft
import dev.ahmad.wird.domain.model.HabitKind
import dev.ahmad.wird.domain.repository.HabitRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Adds a habit, or applies an edit to one, from the edit sheet's [HabitDraft].
 *
 * An edit is two different operations that move in opposite directions through time:
 *
 * - **kind and target** score, so a change opens a new revision **from today** and never
 *   reaches back. Yesterday keeps the target it was actually kept against.
 * - **name and icon** are presentational, so a change applies to **every** revision, the
 *   way order does — a renamed habit reads the same on every day it appears.
 *
 * An edit that changes neither writes nothing, so saving an untouched sheet does not fill
 * history with empty revisions. Every check runs before the first write, so a rejected
 * draft leaves storage exactly as it was.
 *
 * Returns the id of the habit saved, which for a new habit is minted here.
 */
class SaveHabitUseCase(
    private val habits: HabitRepository,
    private val clock: Clock,
    private val zone: TimeZone,
    private val newId: () -> String,
) {
    suspend operator fun invoke(draft: HabitDraft): String {
        val name = draft.name.trim()
        require(name.isNotEmpty()) { "habit name must not be blank" }

        // Switching the sheet from counter to done/not-done can leave the old counter target
        // behind; a BOOL is worth exactly one point whatever the field last said.
        val target = if (draft.kind == HabitKind.BOOL) 1 else draft.target

        val today = clock.now().toLocalDateTime(zone).date
        val active = habits.observeActiveHabits().first()

        return if (draft.id == null) {
            add(draft, name, target, today, active)
        } else {
            edit(draft, draft.id, name, target, today, active)
        }
    }

    private suspend fun add(
        draft: HabitDraft,
        name: String,
        target: Int,
        today: LocalDate,
        active: List<Habit>,
    ): String {
        // Constructed before anything is written: Habit's own checks reject a bad target here.
        val habit = Habit(
            id = newId(),
            name = name,
            kind = draft.kind,
            target = target,
            iconKey = draft.iconKey,
            // A new habit joins the end of the list rather than jumping the user's order.
            sortOrder = (active.maxOfOrNull { it.sortOrder } ?: -1) + 1,
            // Starting any earlier would show a fresh habit a history of missed days.
            effectiveFrom = today,
        )
        habits.upsert(habit)
        return habit.id
    }

    private suspend fun edit(
        draft: HabitDraft,
        id: String,
        name: String,
        target: Int,
        today: LocalDate,
        active: List<Habit>,
    ): String {
        val current = requireNotNull(active.firstOrNull { it.id == id }) {
            "no active habit with id $id; a retired habit has to be reinstated before it is edited"
        }

        val scoringChanged = draft.kind != current.kind || target != current.target
        val appearanceChanged = name != current.name || draft.iconKey != current.iconKey

        // Built before any write, so an invalid target is rejected with storage untouched.
        val revision = if (scoringChanged) {
            current.copy(
                name = name,
                iconKey = draft.iconKey,
                kind = draft.kind,
                target = target,
                // Never before the revision it replaces. Flying west moves today backwards,
                // so a habit made "today" before the flight can start after the new today;
                // opening the edit from today would close the current revision before it
                // began, which storage accepts and every later read then rejects.
                effectiveFrom = maxOf(today, current.effectiveFrom),
                retiredOn = null,
            )
        } else {
            null
        }

        revision?.let { habits.upsert(it) }
        if (appearanceChanged) habits.updateAppearance(id, name, draft.iconKey)

        return id
    }
}
