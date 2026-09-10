package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeEntryRepository
import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.model.HabitKind
import dev.ahmad.wird.domain.usecase.HistoryFixtures.entry
import dev.ahmad.wird.domain.usecase.HistoryFixtures.habit
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Gathers everything the user has recorded and hands back a file to save or share.
 *
 * "Everything" is the point: every revision of every habit, retired ones included, and every
 * entry on every day. An export that quietly left out retired habits would restore a history
 * with days nobody could score.
 */
class ExportDataUseCaseTest {

    private fun useCase(habits: FakeHabitRepository, entries: FakeEntryRepository) =
        ExportDataUseCase(habits, entries, HistoryFixtures.clock, HistoryFixtures.zone)

    private fun JsonObject.count(field: String): Int = getValue(field).jsonArray.size

    private fun parsed(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun includesEveryRevisionOfEveryHabitRetiredOnesToo() = runTest {
        val habits = FakeHabitRepository(
            listOf(
                habit("prayers", HabitKind.COUNTER, target = 3, retiredOn = LocalDate(2026, 1, 10)),
                habit("prayers", HabitKind.COUNTER, target = 5, effectiveFrom = LocalDate(2026, 1, 10)),
                habit("witr", sortOrder = 1, retiredOn = LocalDate(2026, 1, 12)),
            ),
        )

        val document = parsed(useCase(habits, FakeEntryRepository())().contents)

        assertEquals(3, document.count("habits"))
    }

    @Test
    fun includesEveryEntryOnEveryDay() = runTest {
        val entries = FakeEntryRepository(
            listOf(
                entry("prayers", LocalDate(2025, 12, 31), 5),
                entry("prayers", LocalDate(2026, 1, 14), 3),
                entry("duha", LocalDate(2026, 1, 15), 1),
            ),
        )

        val document = parsed(useCase(FakeHabitRepository(), entries)().contents)

        assertEquals(3, document.count("entries"))
    }

    @Test
    fun stampsTheMomentOfExport() = runTest {
        val document = parsed(useCase(FakeHabitRepository(), FakeEntryRepository())().contents)

        assertEquals("2026-01-15T12:00:00Z", document.getValue("exportedAt").jsonPrimitive.content)
    }

    @Test
    fun namesTheFileAfterTheLocalDay() = runTest {
        val file = useCase(FakeHabitRepository(), FakeEntryRepository())()

        assertEquals("wird-export-2026-01-15.json", file.name)
    }

    @Test
    fun namesTheFileAfterTheUsersOwnDayNotTheUtcDay() = runTest {
        // Midday UTC on the 15th is already the small hours of the 16th on Kiritimati. A file
        // named after the UTC day would look a day old to the person who just saved it.
        val kiritimati = UtcOffset(hours = 14).asTimeZone()

        val file = ExportDataUseCase(FakeHabitRepository(), FakeEntryRepository(), HistoryFixtures.clock, kiritimati)()

        assertEquals("wird-export-2026-01-16.json", file.name)
    }

    @Test
    fun exportsAnEmptyInstallAsEmptyLists() = runTest {
        val document = parsed(useCase(FakeHabitRepository(), FakeEntryRepository())().contents)

        assertEquals(0, document.count("habits"))
        assertEquals(0, document.count("entries"))
        assertEquals(1, document.getValue("version").jsonPrimitive.int)
    }

    @Test
    fun failsRatherThanExportingHalfTheDataWhenStorageFails() = runTest {
        // A backup missing everything the failed read would have returned is worse than no
        // backup, because the user would trust it.
        val entries = FakeEntryRepository()
        entries.controls.failWith(IllegalStateException("storage unreadable"))

        assertFailsWith<IllegalStateException> { useCase(FakeHabitRepository(), entries)() }
    }
}
