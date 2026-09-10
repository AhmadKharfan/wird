package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.CircleWeek
import dev.ahmad.wird.domain.model.Leaderboard
import dev.ahmad.wird.domain.model.MemberWeek
import dev.ahmad.wird.domain.model.RankedMember
import dev.ahmad.wird.domain.repository.CircleRepository
import dev.ahmad.wird.domain.repository.SettingsRepository
import dev.ahmad.wird.domain.util.WeekBoundary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * A circle's current week, ranked. Where the circle product rules become behaviour:
 *
 * - **Ranked by percentage, never by raw points.** A member with fewer days scheduled is not
 *   behind for having less to do. Shares are compared exactly, by cross-multiplying the
 *   totals, so a tie is a true tie rather than two floats that happen to differ; tied members
 *   share a rank, the next places are skipped, and ties are ordered by member id so they never
 *   swap places between emissions.
 * - **The current Saturday-to-Friday week.** When Saturday comes the week asked for is a new
 *   one, and everyone starts again from nothing.
 * - **Nothing while the user shares nothing.** "Compare me to myself" means there is nobody to
 *   compare against, so no leaderboard is produced at all, and it returns the moment sharing
 *   resumes.
 * - **Never an error.** A failed refresh ends that read quietly and leaves on screen whatever
 *   was already shown, with its last-updated time; if nothing was ever cached, nothing is shown.
 */
class ObserveLeaderboardUseCase(
    private val circles: CircleRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(circleId: String): Flow<Leaderboard?> {
        val today = clock.now().toLocalDateTime(zone).date
        val weekStart = WeekBoundary.startOfWeek(today)
        val weekEnd = WeekBoundary.endOfWeek(today)

        return settings.observeSettings()
            .map { it.privacyMode.showsCircles }
            .distinctUntilChanged()
            .flatMapLatest { sharing ->
                if (!sharing) {
                    flowOf(null)
                } else {
                    circles.observeWeek(circleId, weekStart)
                        .map { week -> week?.let { ranked(it, weekEnd) } }
                        .catch { }
                }
            }
    }

    private fun ranked(week: CircleWeek, weekEnd: LocalDate): Leaderboard {
        val ordered = week.members.sortedWith(BY_SHARE_DESCENDING.thenBy { it.member.id })

        val members = mutableListOf<RankedMember>()
        ordered.forEachIndexed { index, member ->
            val sharesPlaceWithPrevious = index > 0 && BY_SHARE_DESCENDING.compare(ordered[index - 1], member) == 0
            val rank = if (sharesPlaceWithPrevious) members.last().rank else index + 1
            members += RankedMember(rank = rank, member = member.member, isMe = member.isMe, ratio = member.ratio)
        }

        return Leaderboard(
            circle = week.circle,
            weekStart = week.weekStart,
            weekEnd = weekEnd,
            members = members,
            groupStreak = week.groupStreak,
            refreshedAt = week.refreshedAt,
        )
    }

    private companion object {
        /** Higher share first, compared exactly: a/b against c/d as a*d against c*b. */
        val BY_SHARE_DESCENDING = Comparator<MemberWeek> { first, second ->
            val (firstPoints, firstMax) = first.share()
            val (secondPoints, secondMax) = second.share()
            (secondPoints * firstMax).compareTo(firstPoints * secondMax)
        }

        /** A week with nothing scheduled is a share of 0 of 1, never a division by zero. */
        fun MemberWeek.share(): Pair<Long, Long> =
            if (maxPoints == 0) 0L to 1L else points.toLong() to maxPoints.toLong()
    }
}
