package dev.ahmad.wird.ui.feature.today

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.fake.FakeSettingsRepository
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.HabitKind
import dev.ahmad.wird.domain.model.NumeralSystem
import dev.ahmad.wird.domain.usecase.CalculateDayStatsUseCase
import dev.ahmad.wird.domain.usecase.HistoryFixtures
import dev.ahmad.wird.domain.usecase.ObserveDayUseCase
import dev.ahmad.wird.domain.usecase.ObserveSettingsUseCase
import dev.ahmad.wird.domain.usecase.ObserveTodayUseCase
import dev.ahmad.wird.domain.usecase.SeedDefaultRoutineUseCase
import dev.ahmad.wird.domain.usecase.SetNumeralSystemUseCase
import dev.ahmad.wird.domain.usecase.ToggleHabitUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Today is the one live screen, so it is where "the numeral toggle switches every number in
 * the app through the single formatter" first has to hold. Every number reaches the state
 * already formatted — the composable does no formatting — and a habit name is user text that
 * the formatter never touches.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelNumeralsTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        habits: FakeHabitRepository = FakeHabitRepository(),
    ): TodayViewModel {
        val entries = FakeEntryRepository()
        return TodayViewModel(
            observeToday = ObserveTodayUseCase(
                ObserveDayUseCase(habits, entries),
                HistoryFixtures.clock,
                HistoryFixtures.zone,
            ),
            toggleHabit = ToggleHabitUseCase(entries),
            seedDefaultRoutine = SeedDefaultRoutineUseCase(habits, settings, HistoryFixtures.clock, HistoryFixtures.zone),
            calculateDayStats = CalculateDayStatsUseCase(),
            observeSettings = ObserveSettingsUseCase(settings),
            ioDispatcher = testDispatcher,
        )
    }

    private fun arabicIndicSettings() =
        FakeSettingsRepository(AppSettings.DEFAULTS.copy(numeralSystem = NumeralSystem.ARABIC_INDIC))

    @Test
    fun showsTheScoreInWesternDigitsByDefault() = runTest {
        // A fresh install seeds the fifteen-point routine.
        assertEquals("0 / 15", viewModel().state.value.scoreLabel)
    }

    @Test
    fun showsTheScoreInArabicIndicDigitsWhenChosen() = runTest {
        assertEquals("٠ / ١٥", viewModel(arabicIndicSettings()).state.value.scoreLabel)
    }

    @Test
    fun writesACountersValueInTheChosenDigits() = runTest {
        val prayers = viewModel(arabicIndicSettings()).state.value.rows.single { it.habitId == "five-prayers" }

        assertEquals("٠ / ٥", prayers.valueLabel)
    }

    @Test
    fun switchesEveryNumberTheMomentTheSettingChanges() = runTest {
        val settings = FakeSettingsRepository()
        val today = viewModel(settings)
        assertEquals("0 / 15", today.state.value.scoreLabel)

        SetNumeralSystemUseCase(settings)(NumeralSystem.ARABIC_INDIC)

        assertEquals("٠ / ١٥", today.state.value.scoreLabel)
        assertEquals("٠ / ٥", today.state.value.rows.single { it.habitId == "five-prayers" }.valueLabel)
    }

    @Test
    fun leavesTheDigitsInAHabitNameAsTheUserTypedThem() = runTest {
        // A name is user text, not a number the app produced.
        val habits = FakeHabitRepository(
            listOf(HistoryFixtures.habit("juz", kind = HabitKind.BOOL).copy(name = "جزء 30")),
        )

        val row = viewModel(arabicIndicSettings(), habits).state.value.rows.single()

        assertEquals("جزء 30", row.label)
    }
}
