package dev.ahmad.wird.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * A real [WirdDatabase] held in memory, for tests that need actual SQL rather than a fake.
 *
 * These live in `androidUnitTest` rather than `commonTest` because they run on the host
 * JVM: `BundledSQLiteDriver` needs the JVM variant's desktop natives, and the Android
 * artifact ships `.so` files the JVM cannot load. The SQL under test is identical on both
 * targets, so exercising it here covers wasmJs too.
 *
 * Callers must [WirdDatabase.close] the result; each call is an isolated database.
 */
fun createTestDatabase(): WirdDatabase =
    Room.inMemoryDatabaseBuilder<WirdDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
