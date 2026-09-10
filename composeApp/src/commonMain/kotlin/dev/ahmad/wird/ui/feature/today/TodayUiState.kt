package dev.ahmad.wird.ui.feature.today

import androidx.compose.runtime.Immutable

/**
 * One immutable state object for the screen. `@Immutable` keeps the content composable
 * skippable.
 *
 * Provisional: this is the shape the placeholder screen needs so the app runs on the new
 * data layer. The real state object comes from the Today design board, where the fields
 * are decided by what the screen actually shows.
 */
@Immutable
data class TodayUiState(
    val isLoading: Boolean = true,
    val rows: List<HabitRowUiState> = emptyList(),
    val points: Int = 0,
    val maxPoints: Int = 0,
    val errorMessage: String? = null,
)

@Immutable
data class HabitRowUiState(
    val habitId: String,
    val label: String,
    val valueLabel: String,
    val isComplete: Boolean,
)

/** One-shot events. Never part of state. */
sealed interface TodayEffect {
    data class ShowMessage(val message: String) : TodayEffect
}
