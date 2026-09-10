package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.ExportDocument
import dev.ahmad.wird.domain.model.ExportFile
import dev.ahmad.wird.domain.repository.EntryRepository
import dev.ahmad.wird.domain.repository.HabitRepository
import dev.ahmad.wird.domain.util.ExportJson
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Gathers everything the user has recorded into one JSON file they own.
 *
 * Everything means every revision of every habit, retired ones included, and every entry
 * on every day — a backup has to hold the history the screens no longer show.
 *
 * Both stores are read before anything is encoded, so a failure in either read fails the
 * whole export. A file quietly missing whatever the failed read held would be worse than no
 * file, because the user would trust it.
 *
 * The file is named after the day in the user's own zone. Named after the UTC day, an
 * export made in the small hours could look a day old to the person who just saved it.
 */
class ExportDataUseCase(
    private val habits: HabitRepository,
    private val entries: EntryRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {
    suspend operator fun invoke(): ExportFile {
        val exportedAt = clock.now()
        val document = ExportDocument(
            exportedAt = exportedAt,
            habits = habits.allRevisions(),
            entries = entries.allEntries(),
        )
        val day = exportedAt.toLocalDateTime(zone).date

        return ExportFile(name = "wird-export-$day.json", contents = ExportJson.encode(document))
    }
}
