package dev.ahmad.wird.domain.fake

import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * The knobs every fake repository shares: how slowly it answers, and whether it fails.
 *
 * ViewModels are tested against fakes rather than against Room, so this is where the two
 * situations a real device produces — a slow answer and a failed write — become things a
 * test can ask for directly.
 *
 * [failNextCallWith] exists for the shape that matters most: a tap must update the screen
 * immediately, and then, if the write fails, the state has to roll back. Testing that needs
 * exactly one call to fail and the next to succeed.
 */
class FakeControls {

    /** Applied before every call. Zero by default, so tests are instant unless they ask otherwise. */
    var delay: Duration = Duration.ZERO

    private var persistentFailure: Throwable? = null
    private var oneShotFailure: Throwable? = null
    private var laterFailure: Throwable? = null
    private var callsBeforeFailure = 0

    /** Every subsequent call fails with [error], until [succeed]. */
    fun failWith(error: Throwable) {
        persistentFailure = error
    }

    /** Exactly the next call fails with [error]; the one after it succeeds. */
    fun failNextCallWith(error: Throwable) {
        oneShotFailure = error
    }

    /**
     * The next [calls] calls succeed and every call after them fails with [error], until
     * [succeed]. This is the shape of a failure part-way through a series of writes.
     */
    fun failAfter(calls: Int, error: Throwable) {
        callsBeforeFailure = calls
        laterFailure = error
    }

    /** Clears every failure mode. */
    fun succeed() {
        persistentFailure = null
        oneShotFailure = null
        laterFailure = null
    }

    /** Called by a fake at the start of every operation. */
    suspend fun gate() {
        if (delay > Duration.ZERO) delay(delay)

        oneShotFailure?.let { error ->
            oneShotFailure = null
            throw error
        }
        persistentFailure?.let { throw it }
        laterFailure?.let { error ->
            if (callsBeforeFailure == 0) throw error
            callsBeforeFailure--
        }
    }
}
