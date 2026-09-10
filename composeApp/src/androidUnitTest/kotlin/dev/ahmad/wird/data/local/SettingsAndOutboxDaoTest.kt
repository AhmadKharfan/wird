package dev.ahmad.wird.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Settings is a single row enforced by a pinned primary key, and the outbox is an ordered
 * append-only log. Neither is complicated; both have one property that would be expensive
 * to get wrong.
 */
class SettingsAndOutboxDaoTest {

    private val database = createTestDatabase()
    private val settings get() = database.settingsDao()
    private val outbox get() = database.outboxDao()

    @AfterTest
    fun closeDatabase() = database.close()

    private fun settingsRow(themeMode: String = "SYSTEM", updatedAt: Long = 1) = SettingsEntity(
        themeMode = themeMode,
        numeralSystem = "ARABIC_INDIC",
        latitude = null,
        longitude = null,
        calculationMethod = "UMM_AL_QURA",
        privacyMode = "POINTS_ONLY",
        updatedAt = updatedAt,
    )

    private fun outboxRow(id: String, createdAt: Long, entityId: String = "duha") = OutboxEntity(
        id = id,
        entityType = "entry",
        entityId = entityId,
        op = "upsert",
        payload = """{"value":1}""",
        createdAt = createdAt,
        attempts = 0,
    )

    // --- settings -----------------------------------------------------------------------

    @Test
    fun readsNothingBeforeSettingsAreEverWritten() = runTest {
        // A fresh install has no row; the repository is what supplies the defaults.
        assertNull(settings.get())
        assertNull(settings.observe().first())
    }

    @Test
    fun readsBackTheStoredSettings() = runTest {
        settings.upsert(settingsRow(themeMode = "DARK"))

        assertEquals("DARK", settings.get()?.themeMode)
    }

    @Test
    fun keepsExactlyOneSettingsRowHoweverOftenItIsWritten() = runTest {
        // The pinned primary key is what guarantees this. Without it, every settings
        // change would append and `get` would return whichever row came first.
        settings.upsert(settingsRow(themeMode = "LIGHT", updatedAt = 1))
        settings.upsert(settingsRow(themeMode = "DARK", updatedAt = 2))
        settings.upsert(settingsRow(themeMode = "SYSTEM", updatedAt = 3))

        assertEquals(1, settings.countRows())
        assertEquals("SYSTEM", settings.get()?.themeMode)
    }

    @Test
    fun readsBackASettingsChangeOnTheNextRead() = runTest {
        settings.upsert(settingsRow(themeMode = "LIGHT"))
        assertEquals("LIGHT", settings.observe().first()?.themeMode)

        settings.upsert(settingsRow(themeMode = "DARK", updatedAt = 2))

        assertEquals("DARK", settings.observe().first()?.themeMode)
    }

    // --- outbox --------------------------------------------------------------------------

    @Test
    fun startsEmpty() = runTest {
        assertEquals(0, outbox.count())
    }

    @Test
    fun appendsRatherThanReplacing() = runTest {
        // The outbox is a log of intent. Two changes to one entity are two facts, and
        // collapsing them would lose the order the server needs to replay.
        outbox.append(outboxRow("o1", createdAt = 1))
        outbox.append(outboxRow("o2", createdAt = 2))

        assertEquals(2, outbox.count())
    }

    @Test
    fun peeksInTheOrderRecordsWereCreated() = runTest {
        outbox.append(outboxRow("third", createdAt = 3))
        outbox.append(outboxRow("first", createdAt = 1))
        outbox.append(outboxRow("second", createdAt = 2))

        assertEquals(listOf("first", "second", "third"), outbox.peek(10).map { it.id })
    }

    @Test
    fun peeksAtMostTheRequestedNumber() = runTest {
        outbox.append(outboxRow("o1", createdAt = 1))
        outbox.append(outboxRow("o2", createdAt = 2))
        outbox.append(outboxRow("o3", createdAt = 3))

        assertEquals(listOf("o1", "o2"), outbox.peek(2).map { it.id })
    }

    @Test
    fun keepsThePayloadItWasGivenVerbatim() = runTest {
        // The payload is captured at write time on purpose: re-reading the row at drain
        // time would send whatever it says later, not what actually happened.
        outbox.append(outboxRow("o1", createdAt = 1))

        assertEquals("""{"value":1}""", outbox.peek(1).single().payload)
    }
}
