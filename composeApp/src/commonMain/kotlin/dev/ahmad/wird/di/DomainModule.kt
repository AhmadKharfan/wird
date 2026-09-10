package dev.ahmad.wird.di

import dev.ahmad.wird.domain.usecase.CalculateDayStatsUseCase
import dev.ahmad.wird.domain.usecase.CalculateHabitCommitmentUseCase
import dev.ahmad.wird.domain.usecase.CalculateStreakUseCase
import dev.ahmad.wird.domain.usecase.GetPrayerTimesUseCase
import dev.ahmad.wird.domain.usecase.ObserveDayUseCase
import dev.ahmad.wird.domain.usecase.ObserveHabitCommitmentsUseCase
import dev.ahmad.wird.domain.usecase.ObserveMonthHeatmapUseCase
import dev.ahmad.wird.domain.usecase.ObserveRecentDaysUseCase
import dev.ahmad.wird.domain.usecase.ObserveStreakUseCase
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

    // --- a single day -------------------------------------------------------------------
    factory { ObserveDayUseCase(habits = get(), entries = get()) }
    factory { ObserveTodayUseCase(observeDay = get(), clock = get(), zone = get()) }
    factory { ToggleHabitUseCase(entries = get()) }
    factory { SetCounterValueUseCase(entries = get()) }
    factory { SeedDefaultRoutineUseCase(habits = get()) }

    // --- scoring ------------------------------------------------------------------------
    factory { CalculateDayStatsUseCase() }
    factory { CalculateHabitCommitmentUseCase() }
    factory { CalculateStreakUseCase() }

    // --- periods and history --------------------------------------------------------------
    factory { ObserveWeekSummaryUseCase(habits = get(), entries = get(), calculateDayStats = get()) }
    factory { ObserveMonthHeatmapUseCase(habits = get(), entries = get(), calculateDayStats = get()) }
    factory {
        ObserveRecentDaysUseCase(
            habits = get(),
            entries = get(),
            calculateDayStats = get(),
            clock = get(),
            zone = get(),
        )
    }
    factory { ObserveHabitCommitmentsUseCase(habits = get(), entries = get()) }
    factory {
        ObserveStreakUseCase(
            habits = get(),
            entries = get(),
            calculateDayStats = get(),
            calculateStreak = get(),
            clock = get(),
            zone = get(),
        )
    }

    factory { GetPrayerTimesUseCase(repository = get()) }
}
