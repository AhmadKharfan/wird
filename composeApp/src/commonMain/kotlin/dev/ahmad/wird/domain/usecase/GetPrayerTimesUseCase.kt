package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.PrayerTime
import dev.ahmad.wird.domain.repository.PrayerTimesRepository
import kotlinx.datetime.LocalDate

class GetPrayerTimesUseCase(
    private val repository: PrayerTimesRepository,
) {
    suspend operator fun invoke(date: LocalDate, coordinates: Coordinates): List<PrayerTime> =
        repository.timesFor(date, coordinates)
}
