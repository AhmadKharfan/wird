package dev.ahmad.wird.domain.util

import dev.ahmad.wird.domain.model.Entry
import dev.ahmad.wird.domain.model.ExportDocument
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * The export is written by hand in domain/, because checkDomainPurity keeps serialization
 * libraries out of it — and a hand-written JSON writer is exactly where a missed escape turns
 * a backup into a file nothing can open.
 *
 * So every test here parses the output with the real kotlinx.serialization parser, which test
 * sources are free to use. A test passes only if genuine JSON comes out and reads back as
 * exactly what went in.
 *
 * Control characters are built from their code points with Char(...), never written as
 * escapes, so this file itself contains no raw control characters.
 */
class ExportJsonTest {

    private val exportedAt = Instant.parse("2026-01-15T12:00:00Z")

    private fun habit(
        id: String = "prayers",
        name: String = "الصلوات الخمس",
        kind: HabitKind = HabitKind.COUNTER,
        target: Int = 5,
        sortOrder: Int = 0,
        effectiveFrom: LocalDate = LocalDate(2026, 1, 1),
        retiredOn: LocalDate? = null,
    ) = Habit(
        id = id,
        name = name,
        kind = kind,
        target = target,
        iconKey = id,
        sortOrder = sortOrder,
        effectiveFrom = effectiveFrom,
        retiredOn = retiredOn,
    )

    private fun entry(habitId: String, day: LocalDate, value: Int) = Entry(
        id = "$habitId@$day",
        habitId = habitId,
        day = day,
        value = value,
        updatedAt = exportedAt,
    )

    private fun export(habits: List<Habit> = listOf(habit()), entries: List<Entry> = emptyList()) =
        ExportJson.encode(ExportDocument(exportedAt = exportedAt, habits = habits, entries = entries))

    private fun parsed(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private fun JsonObject.firstHabit(): JsonObject = getValue("habits").jsonArray.first().jsonObject

    /** Exports one habit named [name] and reads the name back through the real parser. */
    private fun roundTripName(name: String): String =
        parsed(export(listOf(habit(name = name)))).firstHabit().getValue("name").jsonPrimitive.content

    // --- it is JSON, and says what it is ------------------------------------------------------

    @Test
    fun identifiesItselfAndItsFormatVersion() {
        // A future importer has to know what it is reading before it reads anything else.
        val document = parsed(export())

        assertEquals("wird-export", document.getValue("format").jsonPrimitive.content)
        assertEquals(1, document.getValue("version").jsonPrimitive.int)
    }

    @Test
    fun writesTheMomentOfExportAsAnIsoInstant() {
        assertEquals("2026-01-15T12:00:00Z", parsed(export()).getValue("exportedAt").jsonPrimitive.content)
    }

    @Test
    fun writesAnEmptyExportAsEmptyLists() {
        val document = parsed(export(habits = emptyList(), entries = emptyList()))

        assertEquals(0, document.getValue("habits").jsonArray.size)
        assertEquals(0, document.getValue("entries").jsonArray.size)
    }

    // --- every field of a habit and an entry -------------------------------------------------

    @Test
    fun writesEveryFieldOfAHabit() {
        val written = parsed(export(listOf(habit(retiredOn = LocalDate(2026, 1, 20))))).firstHabit()

        assertEquals("prayers", written.getValue("id").jsonPrimitive.content)
        assertEquals("الصلوات الخمس", written.getValue("name").jsonPrimitive.content)
        assertEquals("COUNTER", written.getValue("kind").jsonPrimitive.content)
        assertEquals(5, written.getValue("target").jsonPrimitive.int)
        assertEquals("prayers", written.getValue("iconKey").jsonPrimitive.content)
        assertEquals(0, written.getValue("sortOrder").jsonPrimitive.int)
        assertEquals("2026-01-01", written.getValue("effectiveFrom").jsonPrimitive.content)
        assertEquals("2026-01-20", written.getValue("retiredOn").jsonPrimitive.content)
    }

    @Test
    fun writesAnOpenRevisionAsRetiredOnNull() {
        assertEquals(JsonNull, parsed(export()).firstHabit().getValue("retiredOn"))
    }

    @Test
    fun writesEveryFieldOfAnEntry() {
        val jan15 = LocalDate(2026, 1, 15)
        val written = parsed(export(entries = listOf(entry("prayers", jan15, 3))))
            .getValue("entries").jsonArray.single().jsonObject

        assertEquals("prayers@2026-01-15", written.getValue("id").jsonPrimitive.content)
        assertEquals("prayers", written.getValue("habitId").jsonPrimitive.content)
        assertEquals("2026-01-15", written.getValue("day").jsonPrimitive.content)
        assertEquals(3, written.getValue("value").jsonPrimitive.int)
        assertEquals("2026-01-15T12:00:00Z", written.getValue("updatedAt").jsonPrimitive.content)
    }

    @Test
    fun keepsEveryRevisionOfARevisedHabit() {
        // A backup that kept only the current target would restore a history scored against
        // a target the user never had on those days.
        val revisions = listOf(
            habit(target = 3, retiredOn = LocalDate(2026, 1, 10)),
            habit(target = 5, effectiveFrom = LocalDate(2026, 1, 10)),
        )

        val targets = parsed(export(revisions)).getValue("habits").jsonArray
            .map { it.jsonObject.getValue("target").jsonPrimitive.int }

        assertEquals(listOf(3, 5), targets)
    }

    // --- text that would break a careless writer ----------------------------------------------

    @Test
    fun keepsArabicTextIntactIncludingTheHonorific() {
        val name = "مئة صلاة على النبي ﷺ"

        assertEquals(name, roundTripName(name))
    }

    @Test
    fun escapesQuotesAndBackslashesInAName() {
        val quote = Char(0x22)
        val backslash = Char(0x5C)
        val name = "ورد " + quote + "الليل" + quote + " " + backslash + " ب"

        assertEquals(name, roundTripName(name))
    }

    @Test
    fun escapesTheControlCharactersThatHaveAShortForm() {
        // A pasted name can carry a newline or a tab; raw, neither is allowed inside a JSON
        // string at all.
        val name = "سطر" + Char(0x0A) + "ثان" + Char(0x09) + "و" + Char(0x0D) + Char(0x08) + Char(0x0C)

        assertEquals(name, roundTripName(name))
    }

    @Test
    fun escapesAControlCharacterThatHasNoShortForm() {
        // U+0001 has no two-character escape, so it must leave as six characters: a
        // backslash, then u0001. This is asserted on the raw text and not only through the
        // parser, because with the six-character branch deleted a round trip through
        // kotlinx.serialization still passed — that parser accepts a raw control character
        // inside a string, which the specification does not.
        val backslash = Char(0x5C)
        val name = "a" + Char(0x01) + "b" + Char(0x1F)

        val text = export(listOf(habit(name = name)))

        assertEquals(true, text.contains(backslash + "u0001"), "no six-character escape for U+0001 in: $text")
        assertEquals(true, text.contains(backslash + "u001f"), "no six-character escape for U+001F in: $text")
        assertEquals(name, roundTripName(name))
    }

    @Test
    fun leavesNoRawControlCharacterAnywhereInTheOutput() {
        // RFC 8259 forbids every one of them raw inside a string. Checked on the output
        // itself, since the parser used elsewhere here is more forgiving than the spec.
        val everyControlCharacter = (0x00 until 0x20).map { Char(it) }.joinToString("")

        val rawCodes = export(listOf(habit(name = everyControlCharacter)))
            .filter { it.code < 0x20 }
            .map { it.code }

        assertEquals(emptyList(), rawCodes)
    }

    // --- the same data always exports the same way ------------------------------------------

    @Test
    fun writesTheSameBytesWhateverOrderTheDataArrivedIn() {
        // Two exports of the same data can then be compared, and a test can pin the output.
        val a = habit(id = "a", name = "a", sortOrder = 1)
        val b = habit(id = "b", name = "b", sortOrder = 0)
        val jan14 = LocalDate(2026, 1, 14)
        val jan15 = LocalDate(2026, 1, 15)
        val entries = listOf(entry("b", jan15, 1), entry("a", jan14, 1), entry("a", jan15, 1))

        assertEquals(
            export(listOf(a, b), entries),
            export(listOf(b, a), entries.reversed()),
        )
    }

    @Test
    fun ordersHabitsByTheirPlaceInTheListAndEntriesByDay() {
        val document = parsed(
            export(
                habits = listOf(
                    habit(id = "second", name = "second", sortOrder = 1),
                    habit(id = "first", name = "first", sortOrder = 0),
                ),
                entries = listOf(
                    entry("first", LocalDate(2026, 1, 15), 1),
                    entry("first", LocalDate(2026, 1, 14), 1),
                ),
            ),
        )

        assertEquals(
            listOf("first", "second"),
            document.getValue("habits").jsonArray.map { it.jsonObject.getValue("id").jsonPrimitive.content },
        )
        assertEquals(
            listOf("2026-01-14", "2026-01-15"),
            document.getValue("entries").jsonArray.map { it.jsonObject.getValue("day").jsonPrimitive.content },
        )
    }
}
