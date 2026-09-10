package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.Prayer
import dev.ahmad.wird.domain.model.PrayerStatus
import dev.ahmad.wird.domain.repository.PrayerRecordRepository
import kotlinx.datetime.LocalDate

class RecordPrayerUseCase(
    private val repository: PrayerRecordRepository,
) {
    suspend operator fun invoke(date: LocalDate, prayer: Prayer, status: PrayerStatus) {
        repository.record(date, prayer, status)
    }
}
