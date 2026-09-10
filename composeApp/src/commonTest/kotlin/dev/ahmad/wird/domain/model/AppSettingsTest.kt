package dev.ahmad.wird.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The three privacy choices, as the product names them: share points only, use a nickname,
 * or don't share and compare yourself to yourself. Two rules ride on them, and both are
 * enforced in the model so no screen can get them wrong.
 */
class AppSettingsTest {

    private val base = AppSettings.DEFAULTS

    // --- a nickname exists whenever one is shown --------------------------------------------

    @Test
    fun acceptsTheNicknameModeWithANickname() {
        val settings = base.copy(privacyMode = PrivacyMode.NICKNAME, nickname = "أبو عمر")

        assertEquals("أبو عمر", settings.nickname)
    }

    @Test
    fun rejectsTheNicknameModeWithNoNickname() {
        // Otherwise a circle would show a member under no name at all while telling them
        // they chose to be shown by one.
        assertFailsWith<IllegalArgumentException> {
            base.copy(privacyMode = PrivacyMode.NICKNAME, nickname = null)
        }
    }

    @Test
    fun rejectsTheNicknameModeWithABlankNickname() {
        assertFailsWith<IllegalArgumentException> {
            base.copy(privacyMode = PrivacyMode.NICKNAME, nickname = "   ")
        }
    }

    @Test
    fun remembersANicknameWhileAnotherModeIsChosen() {
        // Switching to points-only and back must not make the user type their nickname
        // again, so the nickname survives a mode that does not show it.
        val settings = base.copy(privacyMode = PrivacyMode.POINTS_ONLY, nickname = "أبو عمر")

        assertEquals("أبو عمر", settings.nickname)
    }

    @Test
    fun needsNoNicknameToSharePointsOnlyOrToShareNothing() {
        assertEquals(PrivacyMode.POINTS_ONLY, base.copy(privacyMode = PrivacyMode.POINTS_ONLY, nickname = null).privacyMode)
        assertEquals(PrivacyMode.PRIVATE, base.copy(privacyMode = PrivacyMode.PRIVATE, nickname = null).privacyMode)
    }

    // --- sharing nothing hides circles entirely ----------------------------------------------

    @Test
    fun hidesCirclesWhenSharingNothing() {
        // "Compare me to myself" removes the circle tab rather than greying it out: there is
        // nobody to compare against, so there is nothing to show.
        assertFalse(PrivacyMode.PRIVATE.showsCircles)
    }

    @Test
    fun showsCirclesWheneverSomethingIsShared() {
        assertTrue(PrivacyMode.POINTS_ONLY.showsCircles)
        assertTrue(PrivacyMode.NICKNAME.showsCircles)
    }
}
