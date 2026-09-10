package dev.ahmad.wird.di

import dev.ahmad.wird.ui.feature.today.TodayViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    // Explicit rather than viewModelOf: TodayViewModel takes a Clock and a TimeZone
    // with defaults that Koin must not try to resolve.
    viewModel { TodayViewModel(getPrayerTimes = get(), observeRecords = get(), recordPrayer = get()) }
}
