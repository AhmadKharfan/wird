package dev.ahmad.wird.ui.feature.today

import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.usecase.CalculateDayStatsUseCase
import dev.ahmad.wird.domain.usecase.ObserveTodayUseCase
import dev.ahmad.wird.domain.usecase.SeedDefaultRoutineUseCase
import dev.ahmad.wird.domain.usecase.ToggleHabitUseCase
import dev.ahmad.wird.ui.base.BaseViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Provisional Today screen on the new data layer, replaced when the design board lands.
 *
 * It calls use cases only, never a repository, and holds no calculation of its own —
 * scoring is [CalculateDayStatsUseCase]'s job even here.
 *
 * Seeding runs from this ViewModel because Today is the first screen a fresh install
 * opens. It is idempotent, so running it on every launch costs one query.
 */
class TodayViewModel(
    private val observeToday: ObserveTodayUseCase,
    private val toggleHabit: ToggleHabitUseCase,
    private val seedDefaultRoutine: SeedDefaultRoutineUseCase,
    private val calculateDayStats: CalculateDayStatsUseCase,
    private val clock: Clock,
    private val zone: TimeZone,
) : BaseViewModel<TodayUiState, TodayEffect>(TodayUiState()), TodayInteractionListener {

    private var snapshot: DaySnapshot? = null

    init {
        load()
    }

    override fun onRetry() = load()

    override fun onHabitTapped(habitId: String) {
        val current = snapshot ?: return
        val habit = current.scheduledHabits.firstOrNull { it.id == habitId } ?: return

        tryToExecute(
            block = { toggleHabit(habit, current.day, current.valueFor(habitId)) },
            // The observation re-emits, so there is nothing to apply here.
            onSuccess = { },
            onError = { sendEffect(TodayEffect.ShowMessage(FAILED_TO_SAVE)) },
        )
    }

    private fun load() {
        updateState { it.copy(isLoading = true, errorMessage = null) }

        tryToExecute(
            block = { seedDefaultRoutine(clock.now().toLocalDateTime(zone).date) },
            onSuccess = { observeSnapshot() },
            onError = { updateState { state -> state.copy(isLoading = false, errorMessage = FAILED_TO_LOAD) } },
        )
    }

    private fun observeSnapshot() {
        collectFlow(
            flow = observeToday(),
            onEach = { emitted ->
                snapshot = emitted
                render(emitted)
            },
            onError = {
                updateState { state -> state.copy(isLoading = false, errorMessage = FAILED_TO_LOAD) }
            },
        )
    }

    private fun render(emitted: DaySnapshot) {
        val stats = calculateDayStats(emitted)

        updateState { state ->
            state.copy(
                isLoading = false,
                errorMessage = null,
                points = stats.points,
                maxPoints = stats.maxPoints,
                rows = emitted.scheduledHabits.map { habit ->
                    val value = emitted.valueFor(habit.id)
                    HabitRowUiState(
                        habitId = habit.id,
                        label = habit.name,
                        valueLabel = habit.valueLabel(value),
                        isComplete = value >= habit.target,
                    )
                },
            )
        }
    }
}
