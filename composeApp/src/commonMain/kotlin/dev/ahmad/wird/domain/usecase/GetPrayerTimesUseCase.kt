package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.PrayerTime
import dev.ahmad.wird.domain.repository.PrayerTimesRepository
import dev.ahmad.wird.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/** The day's prayer times, under the calculation method the user chose in settings. */
class GetPrayerTimesUseCase(
    private val repository: PrayerTimesRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(date: LocalDate, coordinates: Coordinates): List<PrayerTime> =
        repository.timesFor(date, coordinates, settings.observeSettings().first().calculationMethod)
}
