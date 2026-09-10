package dev.ahmad.wird.di

import dev.ahmad.wird.ui.AppViewModel
import dev.ahmad.wird.ui.feature.today.TodayViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { AppViewModel(observeSettings = get()) }

    viewModel {
        TodayViewModel(
            observeToday = get(),
            toggleHabit = get(),
            seedDefaultRoutine = get(),
            calculateDayStats = get(),
            observeSettings = get(),
            clock = get(),
            zone = get(),
        )
    }
}
