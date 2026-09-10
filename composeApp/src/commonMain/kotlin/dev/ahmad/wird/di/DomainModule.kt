package dev.ahmad.wird.di

import dev.ahmad.wird.domain.usecase.GetPrayerTimesUseCase
import dev.ahmad.wird.domain.usecase.ObservePrayerRecordsUseCase
import dev.ahmad.wird.domain.usecase.RecordPrayerUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetPrayerTimesUseCase(repository = get()) }
    factory { ObservePrayerRecordsUseCase(repository = get()) }
    factory { RecordPrayerUseCase(repository = get()) }
}
