package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

/**
 * What a circle can see of its members is decided by these two types, so the product rules
 * that are about visibility live here rather than in any screen. A member is a pair of
 * totals — there is no field that could say which habit they did or missed.
 */
class CircleWeekTest {

    private val saturday = LocalDate(2026, 1, 10)
    private val tolerance = 1e-6f

    private val circle = Circle(id = "c", name = "حلقة الفجر", inviteCode = "FAJR", memberCount = 2)

    private fun member(id: String, points: Int, maxPoints: Int, isMe: Boolean = false) =
        MemberWeek(member = CircleMember(id = id, displayName = id), isMe = isMe, points = points, maxPoints = maxPoints)

    private fun week(
        weekStart: LocalDate = saturday,
        members: List<MemberWeek> = listOf(member("a", 1, 2), member("me", 1, 2, isMe = true)),
        groupStreak: Int = 0,
    ) = CircleWeek(
        circle = circle,
        weekStart = weekStart,
        members = members,
        groupStreak = groupStreak,
        refreshedAt = Instant.parse("2026-01-15T12:00:00Z"),
    )

    // --- a member is two totals -------------------------------------------------------

    @Test
    fun turnsAMembersTotalsIntoAShareOfTheWeek() {
        // 83 of 90 is 0.9222…, derived by hand.
        assertEquals(0.92222f, member("a", 83, 90).ratio, 1e-4f)
    }

    @Test
    fun scoresAMemberWithNothingScheduledAsZeroRatherThanDividingByZero() {
        assertEquals(0f, member("a", 0, 0).ratio, tolerance)
    }

    @Test
    fun rejectsMorePointsThanTheWeekHeld() {
        assertFailsWith<IllegalArgumentException> { member("a", 91, 90) }
    }

    @Test
    fun rejectsNegativeTotals() {
        assertFailsWith<IllegalArgumentException> { member("a", -1, 90) }
        assertFailsWith<IllegalArgumentException> { member("a", 0, -1) }
    }

    // --- a week is a Saturday week ------------------------------------------------------

    @Test
    fun acceptsAWeekThatStartsOnSaturday() {
        assertEquals(saturday, week().weekStart)
    }

    @Test
    fun rejectsAWeekThatStartsOnAnyOtherDay() {
        // The week runs Saturday to Friday for everyone in the circle; a week anchored on
        // another day would compare totals over different spans.
        assertFailsWith<IllegalArgumentException> { week(weekStart = LocalDate(2026, 1, 11)) }
    }

    @Test
    fun rejectsTwoMembersBothMarkedAsMe() {
        assertFailsWith<IllegalArgumentException> {
            week(members = listOf(member("a", 1, 2, isMe = true), member("b", 1, 2, isMe = true)))
        }
    }

    @Test
    fun rejectsANegativeGroupStreak() {
        assertFailsWith<IllegalArgumentException> { week(groupStreak = -1) }
    }
}
