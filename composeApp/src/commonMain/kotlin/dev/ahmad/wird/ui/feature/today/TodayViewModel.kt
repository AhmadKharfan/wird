package dev.ahmad.wird.ui.feature.today

import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.Prayer
import dev.ahmad.wird.domain.model.PrayerRecord
import dev.ahmad.wird.domain.model.PrayerStatus
import dev.ahmad.wird.domain.model.PrayerTime
import dev.ahmad.wird.domain.usecase.GetPrayerTimesUseCase
import dev.ahmad.wird.domain.usecase.ObservePrayerRecordsUseCase
import dev.ahmad.wird.domain.usecase.RecordPrayerUseCase
import dev.ahmad.wird.ui.base.BaseViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class TodayViewModel(
    private val getPrayerTimes: GetPrayerTimesUseCase,
    private val observeRecords: ObservePrayerRecordsUseCase,
    private val recordPrayer: RecordPrayerUseCase,
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : BaseViewModel<TodayUiState, TodayEffect>(TodayUiState()), TodayInteractionListener {

    // Placeholder until the location pack lands; Mecca keeps the screen truthful.
    private val coordinates = Coordinates(latitude = 21.4225, longitude = 39.8262)

    private val today: LocalDate get() = clock.now().toLocalDateTime(zone).date

    private var times: List<PrayerTime> = emptyList()
    private var records: List<PrayerRecord> = emptyList()

    init {
        load()
    }

    override fun onPrayerTapped(prayer: Prayer) {
        tryToExecute(
            block = { recordPrayer(today, prayer, PrayerStatus.ON_TIME) },
            onSuccess = { },
            onError = { sendEffect(TodayEffect.ShowMessage(FAILED_TO_RECORD)) },
        )
    }

    override fun onRetry() = load()

    private fun load() {
        updateState { it.copy(isLoading = true, errorMessage = null) }

        tryToExecute(
            block = { getPrayerTimes(today, coordinates) },
            onSuccess = { loaded ->
                times = loaded
                rebuildRows()
                observeRecordsForToday()
            },
            onError = { updateState { state -> state.copy(isLoading = false, errorMessage = FAILED_TO_LOAD) } },
        )
    }

    private fun observeRecordsForToday() {
        collectFlow(
            flow = observeRecords(today),
            onEach = { updated ->
                records = updated
                rebuildRows()
            },
            onError = { sendEffect(TodayEffect.ShowMessage(FAILED_TO_LOAD)) },
        )
    }

    private fun rebuildRows() {
        val statuses = records.associate { it.prayer to it.status }
        updateState { state ->
            state.copy(
                isLoading = false,
                rows = times.map { time ->
                    PrayerRowUiState(
                        prayer = time.prayer,
                        label = time.prayer.arabicLabel(),
                        dueAtLabel = time.dueAt.toLocalDateTime(zone).timeLabel(),
                        status = statuses[time.prayer] ?: PrayerStatus.NOT_RECORDED,
                    )
                },
            )
        }
    }

    private companion object {
        const val FAILED_TO_LOAD = "تعذّر تحميل المواقيت"
        const val FAILED_TO_RECORD = "تعذّر حفظ التسجيل"
    }
}
