package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import dev.ahmad.wird.domain.repository.EntryRepository
import kotlinx.datetime.LocalDate

/**
 * Keeps the direct-entry path inside the counter's meaningful range, so storage cannot
 * accept a value the habit or its UI could never represent.
 */
class SetCounterValueUseCase(private val entries: EntryRepository) {
    suspend operator fun invoke(habit: Habit, day: LocalDate, value: Int) {
        require(habit.kind == HabitKind.COUNTER) { "only a COUNTER habit has a value to set" }
        require(value >= 0) { "counter value must not be negative, was $value" }
        require(value <= habit.target) {
            "counter value must not exceed target ${habit.target}, was $value"
        }

        entries.setValue(habit.id, day, value)
    }
}
