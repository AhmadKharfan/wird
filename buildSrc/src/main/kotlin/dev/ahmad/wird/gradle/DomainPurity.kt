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
     * Fully-qualified references written inline, which carry no import line and would
     * otherwise slip past the import check.
     */
    private val INLINE_REFERENCE = Regex(
        """\b(androidx\.[\w.]+|android\.[\w.]+|org\.jetbrains\.compose\.[\w.]+|org\.koin\.[\w.]+|io\.github\.jan\.[\w.]+)""",
    )

    /**
     * @param source the full text of one Kotlin file in the domain layer.
     * @param allowedPrefixes import prefixes the domain layer may use.
     * @return every violation found, in line order; empty when the file is clean.
     */
    fun check(source: String, allowedPrefixes: List<String>): List<DomainViolation> {
        val violations = mutableListOf<DomainViolation>()

        source.lines().forEachIndexed { index, line ->
            val lineNumber = index + 1
            val imported = IMPORT.find(line)?.groupValues?.get(1)

            if (imported != null) {
                if (allowedPrefixes.none { imported.startsWith(it) }) {
                    violations += DomainViolation(lineNumber, "forbidden import", imported)
                }
                return@forEachIndexed
            }

            if (line.trimStart().startsWith("//")) return@forEachIndexed

            INLINE_REFERENCE.find(line)?.let { match ->
                violations += DomainViolation(lineNumber, "forbidden reference", match.value)
            }
        }

        return violations
    }
}
