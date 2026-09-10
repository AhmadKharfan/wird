package dev.ahmad.wird.ui.feature.today

import androidx.compose.runtime.Immutable
import dev.ahmad.wird.domain.model.Prayer
import dev.ahmad.wird.domain.model.PrayerStatus

/**
 * One immutable state object for the screen. `@Immutable` keeps the content
 * composable skippable.
 */
@Immutable
data class TodayUiState(
    val isLoading: Boolean = true,
    val rows: List<PrayerRowUiState> = emptyList(),
    val errorMessage: String? = null,
)

@Immutable
data class PrayerRowUiState(
    val prayer: Prayer,
    val label: String,
    val dueAtLabel: String,
    val status: PrayerStatus,
)

/** One-shot events. Never part of state. */
sealed interface TodayEffect {
    data class ShowMessage(val message: String) : TodayEffect
}
