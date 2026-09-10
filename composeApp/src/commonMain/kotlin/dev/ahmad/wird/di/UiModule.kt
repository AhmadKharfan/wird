package dev.ahmad.wird.di

import dev.ahmad.wird.ui.feature.today.TodayViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel {
        TodayViewModel(
            observeToday = get(),
            toggleHabit = get(),
            seedDefaultRoutine = get(),
            calculateDayStats = get(),
            clock = get(),
            zone = get(),
        )
    }
}
