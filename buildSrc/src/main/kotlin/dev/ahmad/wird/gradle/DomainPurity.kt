package dev.ahmad.wird.gradle

/**
 * A single breach of the domain layer's dependency rule.
 *
 * @param line 1-based line number within the offending file.
 * @param kind human-readable category, used in the failure message.
 * @param detail the offending import or reference.
 */
data class DomainViolation(
    val line: Int,
    val kind: String,
    val detail: String,
) {
    override fun toString(): String = "$kind: $detail"
}

/**
 * The dependency rule for the domain layer, as pure logic so it can be unit tested
 * without running Gradle.
 *
 * The rule is an allowlist, not a blocklist: anything the domain layer imports must sit
 * under one of the allowed prefixes. A blocklist would silently pass every dependency
 * nobody thought to forbid.
 */
object DomainPurity {

    private val IMPORT = Regex("""^\s*import\s+([\w.]+)""")

    /**
     * A type written with its package inline — lowercase segments, then a capitalised name —
     * which carries no import line and would otherwise slip past the import check. It is
     * judged by the same allowlist as an import. A property chain such as
     * `snapshot.day.plusDays` never ends in a capitalised name, so it is not mistaken for one.
     */
    private val QUALIFIED_TYPE = Regex("""\b[a-z]\w*(?:\.[a-z]\w*)+\.[A-Z]\w*""")

    /**
     * Known outside packages reached through a function rather than a type, such as
     * `org.koin.core.context.startKoin()`, which [QUALIFIED_TYPE] cannot see.
     */
    private val KNOWN_OUTSIDE_PACKAGE = Regex(
        """\b(androidx\.[\w.]+|android\.[\w.]+|org\.jetbrains\.compose\.[\w.]+|org\.koin\.[\w.]+|io\.github\.jan\.[\w.]+)""",
    )

    /**
     * @param source the full text of one Kotlin file in the domain layer.
     * @param allowedPrefixes import prefixes the domain layer may use.
     * @return every violation found, in line order; empty when the file is clean.
     */
    fun check(source: String, allowedPrefixes: List<String>): List<DomainViolation> {
        val violations = mutableListOf<DomainViolation>()

        source.lines().forEachIndexed { index, written ->
            val lineNumber = index + 1
            // Any identifier may be escaped in backticks; unescaped, it is the same name.
            val line = written.replace("`", "")
            val imported = IMPORT.find(line)?.groupValues?.get(1)

            if (imported != null) {
                if (allowedPrefixes.none { imported.startsWith(it) }) {
                    violations += DomainViolation(lineNumber, "forbidden import", imported)
                }
                return@forEachIndexed
            }

            if (line.isComment()) return@forEachIndexed

            (QUALIFIED_TYPE.findAll(line) + KNOWN_OUTSIDE_PACKAGE.findAll(line))
                .map { it.value }
                .filter { reference -> allowedPrefixes.none { reference.startsWith(it) } }
                .distinct()
                .forEach { reference ->
                    violations += DomainViolation(lineNumber, "forbidden reference", reference)
                }
        }

        return violations
    }

    /** A line comment, or a line inside a KDoc or block comment. */
    private fun String.isComment(): Boolean {
        val trimmed = trimStart()
        return trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")
    }
}
