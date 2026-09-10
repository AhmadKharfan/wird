package dev.ahmad.wird.domain.util

import dev.ahmad.wird.domain.model.Entry
import dev.ahmad.wird.domain.model.ExportDocument
import dev.ahmad.wird.domain.model.Habit

/**
 * Writes an [ExportDocument] as JSON, by hand.
 *
 * By hand because checkDomainPurity keeps serialization libraries out of domain/, and the
 * export lives here so that both platforms share one definition of the format. The price of
 * writing JSON by hand is escaping, so every string goes through [quoted], which escapes all
 * that RFC 8259 requires: the quotation mark, the backslash, and every control character
 * below U+0020. Everything else — Arabic included — is written as it is, which JSON allows.
 *
 * The output is deterministic: habits by their place in the list, then id, then start date;
 * entries by day, then habit. Two exports of the same data are byte-identical, so they can
 * be compared, and a test can pin one.
 */
object ExportJson {

    /** Bumped whenever the shape changes, so a future importer knows what it is reading. */
    const val FORMAT_VERSION: Int = 1

    private const val FORMAT = "wird-export"

    fun encode(document: ExportDocument): String = obj(
        "format" to quoted(FORMAT),
        "version" to FORMAT_VERSION.toString(),
        "exportedAt" to quoted(document.exportedAt.toString()),
        "habits" to array(document.habits.sortedWith(HABIT_ORDER).map(::habit)),
        "entries" to array(document.entries.sortedWith(ENTRY_ORDER).map(::entry)),
    )

    private fun habit(habit: Habit): String = obj(
        "id" to quoted(habit.id),
        "name" to quoted(habit.name),
        "kind" to quoted(habit.kind.name),
        "target" to habit.target.toString(),
        "iconKey" to quoted(habit.iconKey),
        "sortOrder" to habit.sortOrder.toString(),
        "effectiveFrom" to quoted(habit.effectiveFrom.toString()),
        "retiredOn" to (habit.retiredOn?.let { quoted(it.toString()) } ?: "null"),
    )

    private fun entry(entry: Entry): String = obj(
        "id" to quoted(entry.id),
        "habitId" to quoted(entry.habitId),
        "day" to quoted(entry.day.toString()),
        "value" to entry.value.toString(),
        "updatedAt" to quoted(entry.updatedAt.toString()),
    )

    private fun obj(vararg fields: Pair<String, String>): String =
        fields.joinToString(separator = ",", prefix = "{", postfix = "}") { (name, value) ->
            quoted(name) + ":" + value
        }

    private fun array(items: List<String>): String =
        items.joinToString(separator = ",", prefix = "[", postfix = "]")

    /** A JSON string literal, escaped as RFC 8259 requires and no further. */
    private fun quoted(text: String): String = buildString(text.length + 2) {
        append('"')
        text.forEach { char ->
            val shortForm = SHORT_ESCAPES[char.code]
            when {
                shortForm != null -> append(shortForm)
                // Every other control character is illegal raw inside a JSON string and has
                // no two-character escape, so it gets the six-character one.
                char.code < FIRST_PRINTABLE -> append("\\u").append(char.code.toString(16).padStart(4, '0'))
                else -> append(char)
            }
        }
        append('"')
    }

    private const val FIRST_PRINTABLE = 0x20

    /**
     * The escapes RFC 8259 gives a two-character form, keyed by code point rather than by
     * character literal so that no control character has to appear in this source file.
     */
    private val SHORT_ESCAPES: Map<Int, String> = mapOf(
        0x22 to "\\\"", // quotation mark
        0x5C to "\\\\", // reverse solidus
        0x08 to "\\b", // backspace
        0x0C to "\\f", // form feed
        0x0A to "\\n", // line feed
        0x0D to "\\r", // carriage return
        0x09 to "\\t", // character tabulation
    )

    private val HABIT_ORDER = compareBy<Habit>({ it.sortOrder }, { it.id }, { it.effectiveFrom })
    private val ENTRY_ORDER = compareBy<Entry>({ it.day }, { it.habitId }, { it.id })
}
