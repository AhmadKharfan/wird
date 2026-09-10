package dev.ahmad.wird.domain.model

import kotlin.time.Instant

/** One prayer's due moment on a given day. */
data class PrayerTime(
    val prayer: Prayer,
    val dueAt: Instant,
)
