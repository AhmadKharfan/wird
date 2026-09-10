package dev.ahmad.wird.ui.feature.today

import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.usecase.CalculateDayStatsUseCase
import dev.ahmad.wird.domain.usecase.ObserveSettingsUseCase
import dev.ahmad.wird.domain.usecase.ObserveTodayUseCase
import dev.ahmad.wird.domain.usecase.SeedDefaultRoutineUseCase
import dev.ahmad.wird.domain.usecase.ToggleHabitUseCase
import dev.ahmad.wird.ui.base.BaseViewModel
import dev.ahmad.wird.ui.format.NumeralFormatter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Provisional Today screen on the new data layer, replaced when the design board lands.
 *
 * It calls use cases only, never a repository, and holds no calculation of its own —
 * scoring is [CalculateDayStatsUseCase]'s job even here. Every number reaches the state
 * already written in the digits the settings choose, through the one [NumeralFormatter], so
 * the composable does no formatting and switching digits repaints the screen at once.
 *
 * Seeding runs from this ViewModel because Today is the first screen a fresh install
 * opens. It is idempotent, so running it on every launch costs one query.
 */
class TodayViewModel(
    private val observeToday: ObserveTodayUseCase,
    private val toggleHabit: ToggleHabitUseCase,
    private val seedDefaultRoutine: SeedDefaultRoutineUseCase,
    private val calculateDayStats: CalculateDayStatsUseCase,
    private val observeSettings: ObserveSettingsUseCase,
    private val clock: Clock,
    private val zone: TimeZone,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
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
            dispatcher = ioDispatcher,
        )
    }

    private fun load() {
        updateState { it.copy(isLoading = true, errorMessage = null) }

        tryToExecute(
            block = { seedDefaultRoutine(clock.now().toLocalDateTime(zone).date) },
            onSuccess = { observeSnapshot() },
            onError = { updateState { state -> state.copy(isLoading = false, errorMessage = FAILED_TO_LOAD) } },
            dispatcher = ioDispatcher,
        )
    }

    private fun observeSnapshot() {
        collectFlow(
            flow = combine(observeToday(), observeSettings()) { day, settings ->
                day to NumeralFormatter(settings.numeralSystem)
            },
            onEach = { (emitted, numerals) ->
                snapshot = emitted
                render(emitted, numerals)
            },
            onError = {
                updateState { state -> state.copy(isLoading = false, errorMessage = FAILED_TO_LOAD) }
            },
        )
    }

    private fun render(emitted: DaySnapshot, numerals: NumeralFormatter) {
        val stats = calculateDayStats(emitted)

        updateState { state ->
            state.copy(
                isLoading = false,
                errorMessage = null,
                scoreLabel = scoreLabel(stats.points, stats.maxPoints, numerals),
                rows = emitted.scheduledHabits.map { habit ->
                    val value = emitted.valueFor(habit.id)
                    HabitRowUiState(
                        habitId = habit.id,
                        // User text, so never passed through the formatter.
                        label = habit.name,
                        valueLabel = habit.valueLabel(value, numerals),
                        isComplete = value >= habit.target,
                    )
                },
            )
        }
    }
}
