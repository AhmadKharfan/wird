package dev.ahmad.wird

import android.app.Application
import dev.ahmad.wird.di.initKoin

class WirdApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(platformModule = androidPlatformModule(applicationContext))
    }
}
