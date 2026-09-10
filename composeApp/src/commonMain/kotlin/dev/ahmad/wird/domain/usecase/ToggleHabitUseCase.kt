package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import dev.ahmad.wird.domain.repository.EntryRepository
import kotlinx.datetime.LocalDate

/**
 * Interprets one tap according to the habit kind instead of leaving that rule to the UI.
 * A counter wraps after its target because that is the only way to undo an over-tap
 * without adding a second control.
 */
class ToggleHabitUseCase(private val entries: EntryRepository) {
    suspend operator fun invoke(habit: Habit, day: LocalDate, currentValue: Int) {
        require(currentValue >= 0) { "current value must not be negative, was $currentValue" }

        when (habit.kind) {
            HabitKind.BOOL -> entries.toggle(habit.id, day)
            HabitKind.COUNTER -> {
                val next = if (habit.isKeptBy(currentValue)) 0 else currentValue + 1
                entries.setValue(habit.id, day, next)
            }
        }
    }
}
