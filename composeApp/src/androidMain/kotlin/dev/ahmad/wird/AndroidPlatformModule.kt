package dev.ahmad.wird

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.ahmad.wird.data.local.WirdDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidPlatformModule(context: Context): Module = module {
    single<RoomDatabase.Builder<WirdDatabase>> {
        Room.databaseBuilder<WirdDatabase>(
            context = context,
            name = context.getDatabasePath(DATABASE_NAME).absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
    }
}

private const val DATABASE_NAME = "wird.db"
