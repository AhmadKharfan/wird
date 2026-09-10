package dev.ahmad.wird.di

import androidx.room3.RoomDatabase
import dev.ahmad.wird.data.local.WirdDatabase
import dev.ahmad.wird.data.repository.AdhanPrayerTimesRepository
import dev.ahmad.wird.data.repository.EntryRepositoryImpl
import dev.ahmad.wird.data.repository.HabitRepositoryImpl
import dev.ahmad.wird.data.repository.OutboxWriter
import dev.ahmad.wird.data.repository.SettingsRepositoryImpl
import dev.ahmad.wird.domain.repository.EntryRepository
import dev.ahmad.wird.domain.repository.HabitRepository
import dev.ahmad.wird.domain.repository.PrayerTimesRepository
import dev.ahmad.wird.domain.repository.SettingsRepository
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Shared data wiring. The configured [RoomDatabase.Builder] comes from the platform
 * module passed to [initKoin], because Android needs a Context and wasmJs needs a
 * Web Worker.
 */
@OptIn(ExperimentalUuidApi::class)
val dataModule = module {
    single<WirdDatabase> { get<RoomDatabase.Builder<WirdDatabase>>().build() }

    single { get<WirdDatabase>().habitDao() }
    single { get<WirdDatabase>().entryDao() }
    single { get<WirdDatabase>().settingsDao() }
    single { get<WirdDatabase>().outboxDao() }

    single<Clock> { Clock.System }
    // Row ids are minted on the client so a row keeps one identity from creation onward,
    // including rows created with no network in sight.
    single<() -> String> { { Uuid.random().toString() } }

    single { OutboxWriter(dao = get(), clock = get(), newId = get()) }

    single<HabitRepository> {
        HabitRepositoryImpl(habits = get(), outbox = get(), clock = get(), newId = get())
    }
    single<EntryRepository> {
        EntryRepositoryImpl(entries = get(), outbox = get(), clock = get(), newId = get())
    }
    single<SettingsRepository> {
        SettingsRepositoryImpl(settings = get(), outbox = get(), clock = get(), newId = get())
    }

    single<PrayerTimesRepository> { AdhanPrayerTimesRepository() }
}
