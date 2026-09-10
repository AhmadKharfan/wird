package dev.ahmad.wird.domain.model

import kotlin.time.Instant

/**
 * Everything the user has recorded, as of [exportedAt], for a backup they own.
 *
 * Every habit revision is included, retired ones too. A backup that kept only the current
 * targets would restore a history scored against targets the user never had on those days.
 */
data class ExportDocument(
    val exportedAt: Instant,
    val habits: List<Habit>,
    val entries: List<Entry>,
)
