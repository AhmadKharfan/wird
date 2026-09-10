package dev.ahmad.wird.di

import dev.ahmad.wird.domain.usecase.CalculateBadgesUseCase
import dev.ahmad.wird.domain.usecase.CalculateDayStatsUseCase
import dev.ahmad.wird.domain.usecase.CalculateHabitCommitmentUseCase
import dev.ahmad.wird.domain.usecase.CalculateStreakUseCase
import dev.ahmad.wird.domain.usecase.ExportDataUseCase
import dev.ahmad.wird.domain.usecase.GetPrayerTimesUseCase
import dev.ahmad.wird.domain.usecase.LoadBadgesUseCase
import dev.ahmad.wird.domain.usecase.ObserveActiveHabitsUseCase
import dev.ahmad.wird.domain.usecase.ObserveCircleTabVisibleUseCase
import dev.ahmad.wird.domain.usecase.ObserveDayUseCase
import dev.ahmad.wird.domain.usecase.ObserveHabitCommitmentsUseCase
import dev.ahmad.wird.domain.usecase.ObserveLeaderboardUseCase
import dev.ahmad.wird.domain.usecase.ObserveMonthHeatmapUseCase
import dev.ahmad.wird.domain.usecase.ObserveRecentDaysUseCase
import dev.ahmad.wird.domain.usecase.ObserveSettingsUseCase
import dev.ahmad.wird.domain.usecase.ObserveStreakUseCase
import dev.ahmad.wird.domain.usecase.ObserveTodayUseCase
import dev.ahmad.wird.domain.usecase.ObserveWeekSummaryUseCase
import dev.ahmad.wird.domain.usecase.ReorderHabitsUseCase
import dev.ahmad.wird.domain.usecase.SaveHabitUseCase
import dev.ahmad.wird.domain.usecase.SeedDefaultRoutineUseCase
import dev.ahmad.wird.domain.usecase.SetCounterValueUseCase
import dev.ahmad.wird.domain.usecase.SetHabitActiveUseCase
import dev.ahmad.wird.domain.usecase.SetNumeralSystemUseCase
import dev.ahmad.wird.domain.usecase.SetPrivacyModeUseCase
import dev.ahmad.wird.domain.usecase.SetThemeModeUseCase
import dev.ahmad.wird.domain.usecase.ToggleHabitUseCase
import kotlinx.datetime.TimeZone
import org.koin.dsl.module

val domainModule = module {
    // The zone of the user decides which day is today. Resolved per injection rather than
    // cached, so a device that changes timezone is not stuck on yesterday.
    factory<TimeZone> { TimeZone.currentSystemDefault() }

    // --- a single day -------------------------------------------------------------------
    factory { ObserveDayUseCase(habits = get(), entries = get()) }
    factory { ObserveTodayUseCase(observeDay = get(), clock = get(), zone = get()) }
    factory { ToggleHabitUseCase(entries = get()) }
    factory { SetCounterValueUseCase(entries = get()) }
    factory { SeedDefaultRoutineUseCase(habits = get(), settings = get()) }

    // --- managing the routine -------------------------------------------------------------
    factory { ObserveActiveHabitsUseCase(habits = get()) }
    factory { SaveHabitUseCase(habits = get(), clock = get(), zone = get(), newId = get()) }
    factory { ReorderHabitsUseCase(habits = get()) }
    factory { SetHabitActiveUseCase(habits = get(), clock = get(), zone = get()) }

    // --- export ---------------------------------------------------------------------------
    factory { ExportDataUseCase(habits = get(), entries = get(), clock = get(), zone = get()) }

    // --- circles ---------------------------------------------------------------------------
    factory { ObserveLeaderboardUseCase(circles = get(), settings = get(), clock = get(), zone = get()) }

    // --- badges ---------------------------------------------------------------------------
    factory { CalculateBadgesUseCase(calculateDayStats = get()) }
    factory {
        LoadBadgesUseCase(habits = get(), entries = get(), clock = get(), zone = get(), calculateBadges = get())
    }

    // --- preferences ----------------------------------------------------------------------
    factory { ObserveSettingsUseCase(settings = get()) }
    factory { SetThemeModeUseCase(settings = get()) }
    factory { SetNumeralSystemUseCase(settings = get()) }
    factory { SetPrivacyModeUseCase(settings = get()) }
    factory { ObserveCircleTabVisibleUseCase(settings = get()) }

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
