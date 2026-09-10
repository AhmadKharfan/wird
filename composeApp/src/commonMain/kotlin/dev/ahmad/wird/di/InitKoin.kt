package dev.ahmad.wird.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * [platformModule] supplies the RoomDatabase.Builder for the running target: Android
 * needs a Context, wasmJs needs a Web Worker.
 */
fun initKoin(
    platformModule: Module,
    configure: KoinApplication.() -> Unit = {},
) = startKoin {
    configure()
    modules(platformModule, dataModule, domainModule)
}
