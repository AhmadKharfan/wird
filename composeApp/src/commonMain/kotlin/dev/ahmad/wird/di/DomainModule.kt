package dev.ahmad.wird.di

import dev.ahmad.wird.domain.usecase.CalculateDayStatsUseCase
import dev.ahmad.wird.domain.usecase.CalculateHabitCommitmentUseCase
import dev.ahmad.wird.domain.usecase.CalculateStreakUseCase
import dev.ahmad.wird.domain.usecase.GetPrayerTimesUseCase
import dev.ahmad.wird.domain.usecase.ObserveMonthHeatmapUseCase
import dev.ahmad.wird.domain.usecase.ObserveTodayUseCase
import dev.ahmad.wird.domain.usecase.ObserveWeekSummaryUseCase
import dev.ahmad.wird.domain.usecase.SeedDefaultRoutineUseCase
import dev.ahmad.wird.domain.usecase.SetCounterValueUseCase
import dev.ahmad.wird.domain.usecase.ToggleHabitUseCase
import kotlinx.datetime.TimeZone
import org.koin.dsl.module

val domainModule = module {
    // The user's own zone decides which day is today. Resolved per injection rather than
    // cached, so a device that changes timezone is not stuck on yesterday.
    factory<TimeZone> { TimeZone.currentSystemDefault() }

    factory { ObserveTodayUseCase(habits = get(), entries = get(), clock = get(), zone = get()) }
    factory { ToggleHabitUseCase(entries = get()) }
    factory { SetCounterValueUseCase(entries = get()) }
    factory { SeedDefaultRoutineUseCase(habits = get()) }

    factory { CalculateDayStatsUseCase() }
    factory { CalculateHabitCommitmentUseCase() }
    factory { CalculateStreakUseCase() }
    factory { ObserveWeekSummaryUseCase(habits = get(), entries = get(), calculateDayStats = get()) }
    factory { ObserveMonthHeatmapUseCase(habits = get(), entries = get(), calculateDayStats = get()) }

    factory { GetPrayerTimesUseCase(repository = get()) }
}
