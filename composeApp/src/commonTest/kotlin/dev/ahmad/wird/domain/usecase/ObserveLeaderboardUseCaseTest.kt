package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.data.remote.FakeCircleRepository
import dev.ahmad.wird.domain.fake.FakeSettingsRepository
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.Circle
import dev.ahmad.wird.domain.model.CircleMember
import dev.ahmad.wird.domain.model.CircleWeek
import dev.ahmad.wird.domain.model.DayStats
import dev.ahmad.wird.domain.model.Leaderboard
import dev.ahmad.wird.domain.model.MemberWeek
import dev.ahmad.wird.domain.model.PrivacyMode
import dev.ahmad.wird.domain.repository.CircleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Where the circle product rules become behaviour: rank by percentage and never by raw
 * points, read the current Saturday-to-Friday week, show nothing at all while the user shares
 * nothing, and never turn a failed refresh into an error or an empty screen.
 *
 * Today is Thursday 15 January 2026, so the week runs Saturday the 10th to Friday the 16th.
 */
class ObserveLeaderboardUseCaseTest {

    private val saturday = LocalDate(2026, 1, 10)
    private val friday = LocalDate(2026, 1, 16)

    private fun useCase(
        circles: CircleRepository = FakeCircleRepository(HistoryFixtures.clock, HistoryFixtures.zone),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        clock: Clock = HistoryFixtures.clock,
    ) = ObserveLeaderboardUseCase(circles, settings, clock, HistoryFixtures.zone)

    private fun member(id: String, points: Int, maxPoints: Int, isMe: Boolean = false) =
        MemberWeek(CircleMember(id = id, displayName = id), isMe = isMe, points = points, maxPoints = maxPoints)

    /** A repository whose week read is [read]; everything else is the seeded stand-in. */
    private fun reading(read: Flow<CircleWeek?>): CircleRepository = object : CircleRepository by
        FakeCircleRepository(HistoryFixtures.clock, HistoryFixtures.zone) {
        override fun observeWeek(circleId: String, weekStart: LocalDate): Flow<CircleWeek?> = read
    }

    private fun serving(vararg members: MemberWeek): CircleRepository = reading(flowOf(week(*members)))

    private fun week(vararg members: MemberWeek) = CircleWeek(
        circle = Circle(id = "c", name = "c", inviteCode = "C", memberCount = members.size),
        weekStart = saturday,
        members = members.toList(),
        groupStreak = 0,
        refreshedAt = HistoryFixtures.now,
    )

    /**
     * Collects in the background until the scheduler goes idle, and reports what arrived and
     * whether the collector is still alive. The leaderboard follows settings, which never
     * complete, so collecting to the end would never return; and an error escaping the use
     * case would kill the collector, which is exactly what these tests must rule out.
     */
    private fun TestScope.collectUntilIdle(board: Flow<Leaderboard?>): Pair<List<Leaderboard?>, Boolean> {
        val emitted = mutableListOf<Leaderboard?>()
        val collecting = launch(UnconfinedTestDispatcher(testScheduler)) { board.collect { emitted += it } }
        advanceUntilIdle()
        val stillAlive = collecting.isActive
        collecting.cancel()
        return emitted to stillAlive
    }

    // --- ranking ---------------------------------------------------------------------------

    @Test
    fun ranksTheSeededCircleByPercentage() = runTest {
        val board = useCase()("fajr-circle").first()!!

        assertEquals(
            listOf("salim", "yasir", "me", "khalid", "muadh"),
            board.members.map { it.member.id },
        )
        assertEquals(listOf(1, 2, 3, 4, 5), board.members.map { it.rank })
    }

    @Test
    fun ranksByPercentageEvenWhenThatMeansFewerPointsRanksHigher() = runTest {
        // 50 of 60 is 83%; 70 of 90 is 78%. The member with fewer points leads, because a
        // member with fewer days scheduled is not behind for having less to do.
        val board = useCase(serving(member("more-points", 70, 90), member("higher-share", 50, 60)))("c").first()!!

        assertEquals(listOf("higher-share", "more-points"), board.members.map { it.member.id })
    }

    @Test
    fun givesEqualPercentagesTheSameRankAndSkipsThePlacesTheyShare() = runTest {
        val board = useCase(
            serving(
                member("first", 9, 10),
                member("tied-a", 8, 10),
                member("tied-b", 16, 20),
                member("last", 1, 10),
            ),
        )("c").first()!!

        assertEquals(listOf(1, 2, 2, 4), board.members.map { it.rank })
    }

    @Test
    fun ordersTiedMembersTheSameWayEveryTime() = runTest {
        // Equal percentages must not swap places between emissions.
        val board = useCase(serving(member("b", 8, 10), member("a", 16, 20)))("c").first()!!

        assertEquals(listOf("a", "b"), board.members.map { it.member.id })
    }

    // --- the week -------------------------------------------------------------------------------

    @Test
    fun readsTheCurrentSaturdayToFridayWeek() = runTest {
        val board = useCase()("fajr-circle").first()!!

        assertEquals(saturday, board.weekStart)
        assertEquals(friday, board.weekEnd)
    }

    @Test
    fun startsAgainFromNothingWhenANewWeekBegins() = runTest {
        // Saturday the 17th: the same circle, everyone back at zero.
        val nextSaturday = object : Clock {
            override fun now(): Instant = Instant.parse("2026-01-17T12:00:00Z")
        }

        val board = useCase(clock = nextSaturday)("fajr-circle").first()!!

        assertEquals(LocalDate(2026, 1, 17), board.weekStart)
        assertEquals(setOf(0f), board.members.map { it.ratio }.toSet())
    }

    @Test
    fun carriesTheGroupStreakAndWhenTheCopyWasFetched() = runTest {
        val board = useCase()("fajr-circle").first()!!

        assertEquals(4, board.groupStreak)
        assertEquals(HistoryFixtures.now, board.refreshedAt)
    }

    // --- privacy ----------------------------------------------------------------------------------

    @Test
    fun showsNoLeaderboardWhileTheUserSharesNothing() = runTest {
        // "Compare me to myself": the user is not comparing against anyone, so there is no
        // leaderboard to read, even for a circle they belong to.
        val settings = FakeSettingsRepository(AppSettings.DEFAULTS.copy(privacyMode = PrivacyMode.PRIVATE))

        assertNull(useCase(settings = settings)("fajr-circle").first())
    }

    @Test
    fun bringsTheLeaderboardBackTheMomentSharingResumes() = runTest {
        val settings = FakeSettingsRepository(AppSettings.DEFAULTS.copy(privacyMode = PrivacyMode.PRIVATE))
        val seen = mutableListOf<Boolean>()
        val collecting = launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase(settings = settings)("fajr-circle").collect { seen += it != null }
        }

        SetPrivacyModeUseCase(settings)(PrivacyMode.POINTS_ONLY)
        collecting.cancel()

        assertEquals(listOf(false, true), seen)
    }

    // --- offline: never a spinner, never an error ---------------------------------------------------

    @Test
    fun keepsTheLastCopyWhenARefreshFails() = runTest {
        // A copy arrives from the cache, then the refresh behind it fails. The screen keeps the
        // copy it has, with its last-updated time, and hears nothing about the failure.
        val cachedThenFailing = reading(
            flow {
                emit(week(member("a", 8, 10)))
                error("network unreachable")
            },
        )

        val (emitted, stillAlive) = collectUntilIdle(useCase(cachedThenFailing)("c"))

        assertEquals(true, stillAlive)
        assertEquals(listOf("a"), emitted.single()!!.members.map { it.member.id })
    }

    @Test
    fun showsNothingRatherThanAnErrorWhenNothingWasEverCached() = runTest {
        val neverReachable = reading(flow { error("network unreachable") })

        val (emitted, stillAlive) = collectUntilIdle(useCase(neverReachable)("c"))

        assertEquals(true, stillAlive)
        assertEquals(emptyList(), emitted)
    }

    @Test
    fun readsNothingForACircleTheUserIsNotIn() = runTest {
        assertNull(useCase()("no-such-circle").first())
    }

    @Test
    fun reflectsWhatTheUserPublishes() = runTest {
        val circles = FakeCircleRepository(HistoryFixtures.clock, HistoryFixtures.zone)
        circles.publishDay("fajr-circle", LocalDate(2026, 1, 15), DayStats(points = 15, maxPoints = 15))

        val me = useCase(circles)("fajr-circle").first()!!.members.single { it.isMe }

        assertEquals(1, me.rank)
        assertEquals(1f, me.ratio)
    }
}
