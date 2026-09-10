package dev.ahmad.wird

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import dev.ahmad.wird.data.local.WirdDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import org.w3c.dom.Worker

/**
 * androidx.sqlite:sqlite-web ships the Kotlin driver only — no worker and no SQLite
 * WASM binary. Both live in wasmJsMain/resources: `sqlite-worker.js` is androidx's
 * reference worker, and `sqlite3/` is the official @sqlite.org/sqlite-wasm build.
 *
 * The worker is an ES module, so it must be constructed with type: "module".
 * The host must send COOP/COEP headers or OPFS is unavailable.
 */
private fun createModuleWorker(url: String): Worker =
    js("new Worker(url, { type: 'module' })")

fun wasmPlatformModule(): Module = module {
    single<RoomDatabase.Builder<WirdDatabase>> {
        Room.databaseBuilder<WirdDatabase>(name = DATABASE_NAME)
            .setDriver(WebWorkerSQLiteDriver(createModuleWorker("sqlite-worker.js")))
    }
}

private const val DATABASE_NAME = "wird.db"
