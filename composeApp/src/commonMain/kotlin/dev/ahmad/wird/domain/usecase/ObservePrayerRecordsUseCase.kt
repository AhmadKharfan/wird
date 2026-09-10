package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.PrayerRecord
import dev.ahmad.wird.domain.repository.PrayerRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class ObservePrayerRecordsUseCase(
    private val repository: PrayerRecordRepository,
) {
    operator fun invoke(date: LocalDate): Flow<List<PrayerRecord>> =
        repository.observeRecords(date)
}
