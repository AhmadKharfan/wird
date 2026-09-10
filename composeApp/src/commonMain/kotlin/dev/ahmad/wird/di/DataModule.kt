package dev.ahmad.wird.di

import androidx.room3.RoomDatabase
import dev.ahmad.wird.data.local.WirdDatabase
import dev.ahmad.wird.data.repository.AdhanPrayerTimesRepository
import dev.ahmad.wird.data.repository.PrayerRecordRepositoryImpl
import dev.ahmad.wird.domain.repository.PrayerRecordRepository
import dev.ahmad.wird.domain.repository.PrayerTimesRepository
import org.koin.dsl.module

/**
 * Shared data wiring. The configured [RoomDatabase.Builder] comes from the platform
 * module passed to [initKoin], because Android needs a Context and wasmJs needs a
 * Web Worker.
 */
val dataModule = module {
    single<WirdDatabase> { get<RoomDatabase.Builder<WirdDatabase>>().build() }
    single { get<WirdDatabase>().prayerRecordDao() }
    single<PrayerRecordRepository> { PrayerRecordRepositoryImpl(dao = get()) }
    single<PrayerTimesRepository> { AdhanPrayerTimesRepository() }
}
