package dev.ahmad.wird.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

/**
 * Seeds a Room observation with an explicit read, so its first emission never depends on
 * an invalidation arriving.
 *
 * This exists because of a defect measured in the browser build, not a theoretical one.
 * On wasmJs, a Room `Flow` did not deliver its initial value: after a page reload the
 * database plainly contained the day's records, yet the screen stayed empty until the user
 * tapped something, at which point the write's invalidation delivered every stored row at
 * once. Left alone, that means opening the app the next morning shows an empty routine.
 *
 * Two causes fit that behaviour — the invalidation tracker never firing an initial event,
 * or the query running before the web worker finishes opening the database and returning
 * empty without a re-query. A suspending read on subscription fixes both: it waits for the
 * database to be open and emits what is actually stored.
 *
 * [distinctUntilChanged] keeps the seed from showing up as a duplicate on Android, where
 * Room's own initial emission carries the same value.
 *
 * Note for anyone tempted to delete this: no JVM test fails without it, because Room
 * behaves correctly there. That was checked by mutation rather than assumed. The
 * behaviour it protects is only observable in the browser build.
 */
internal fun <T> Flow<T>.seededWith(read: suspend () -> T): Flow<T> =
    onStart { emit(read()) }.distinctUntilChanged()
