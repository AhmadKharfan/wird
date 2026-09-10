package dev.ahmad.wird.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomainPurityTest {

    private val allowed = listOf(
        "kotlin.",
        "kotlinx.datetime.",
        "kotlinx.coroutines.",
        "dev.ahmad.wird.domain.",
    )

    private fun check(source: String) = DomainPurity.check(source.trimIndent(), allowed)

    @Test
    fun acceptsAFileUsingOnlyAllowedPrefixes() {
        val violations = check(
            """
            package dev.ahmad.wird.domain.model

            import kotlin.time.Instant
            import kotlinx.datetime.LocalDate
            import kotlinx.coroutines.flow.Flow
            import dev.ahmad.wird.domain.model.Prayer

            data class Thing(val at: Instant)
            """,
        )

        assertEquals(emptyList(), violations)
    }

    @Test
    fun rejectsARoomImport() {
        val violations = check(
            """
            package dev.ahmad.wird.domain.model

            import androidx.room3.Entity
            """,
        )

        assertEquals(1, violations.size)
        assertEquals(3, violations.single().line)
        assertEquals("forbidden import", violations.single().kind)
        assertEquals("androidx.room3.Entity", violations.single().detail)
    }

    @Test
    fun rejectsReachingIntoDataAndUi() {
        val violations = check(
            """
            package dev.ahmad.wird.domain.model

            import dev.ahmad.wird.data.local.PrayerRecordEntity
            import dev.ahmad.wird.ui.theme.Spacing
            """,
        )

        assertEquals(
            listOf(
                "dev.ahmad.wird.data.local.PrayerRecordEntity",
                "dev.ahmad.wird.ui.theme.Spacing",
            ),
            violations.map { it.detail },
        )
    }

    @Test
    fun rejectsCompseAndKoinImports() {
        val violations = check(
            """
            package dev.ahmad.wird.domain.model

            import androidx.compose.runtime.Composable
            import org.koin.core.module.Module
            """,
        )

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.kind == "forbidden import" })
    }

    @Test
    fun rejectsAFullyQualifiedReferenceWithNoImport() {
        val violations = check(
            """
            package dev.ahmad.wird.domain.model

            data class Thing(val ctx: android.content.Context?)
            """,
        )

        assertEquals(1, violations.size)
        assertEquals("forbidden reference", violations.single().kind)
        assertEquals("android.content.Context", violations.single().detail)
    }

    @Test
    fun ignoresForbiddenNamesInsideComments() {
        val violations = check(
            """
            package dev.ahmad.wird.domain.model

            // deliberately not androidx.room3.Entity - this layer stores nothing
            data class Thing(val id: String)
            """,
        )

        assertEquals(emptyList(), violations)
    }

    @Test
    fun reportsEveryViolationNotJustTheFirst() {
        val violations = check(
            """
            package dev.ahmad.wird.domain.model

            import androidx.room3.Entity
            import org.koin.core.module.Module
            import dev.ahmad.wird.ui.theme.Spacing
            """,
        )

        assertEquals(listOf(3, 4, 5), violations.map { it.line })
    }
}
