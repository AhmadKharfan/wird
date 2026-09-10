package dev.ahmad.wird.data.remote

import dev.ahmad.wird.domain.model.Circle
import dev.ahmad.wird.domain.model.CircleMember
import dev.ahmad.wird.domain.model.CircleWeek
import dev.ahmad.wird.domain.model.DayStats
import dev.ahmad.wird.domain.model.MemberWeek
import dev.ahmad.wird.domain.repository.CircleRepository
import dev.ahmad.wird.domain.util.WeekBoundary
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The stand-in for the circle backend until it exists. It lives where the real client will,
 * in `data/remote/`, and is bound to the same interface, so nothing above the interface can
 * tell the difference.
 *
 * It behaves like a server with a cache: one seeded circle, answers for the current Saturday
 * week and every week after it, nothing cached for weeks before, and a "last refreshed"
 * moment fixed when it was created.
 *
 * The seed is the circle the product was designed around — حلقة الفجر, five members, a group
 * streak of four — with week-so-far totals of 83, 79, 70, 64 and 58 out of 90, which read as
 * 92%, 88%, 78%, 71% and 64%. Members are held in no ranked order on purpose: ranking by
 * percentage is a domain decision, and a repository that happened to sort would hide a
 * missing sort above it.
 */
class FakeCircleRepository(
    clock: Clock,
    zone: TimeZone,
) : CircleRepository {

    private val seededWeek: LocalDate = WeekBoundary.startOfWeek(clock.now().toLocalDateTime(zone).date)
    private val refreshedAt: Instant = clock.now()

    private val state = MutableStateFlow(
        State(
            circles = mapOf(FAJR.id to FAJR),
            myCircleIds = listOf(FAJR.id),
            published = emptyMap(),
            created = 0,
        ),
    )

    override fun observeMyCircles(): Flow<List<Circle>> =
        state.map { current -> current.myCircleIds.mapNotNull { current.circles[it] } }

    override fun observeWeek(circleId: String, weekStart: LocalDate): Flow<CircleWeek?> =
        state.map { current -> weekOf(current, circleId, weekStart) }

    override suspend fun create(name: String): Circle {
        var circle: Circle? = null
        state.update { current ->
            val number = current.created + 1
            val made = Circle(id = "created-$number", name = name, inviteCode = "WIRD-$number", memberCount = 1)
            circle = made
            current.copy(
                circles = current.circles + (made.id to made),
                myCircleIds = current.myCircleIds + made.id,
                created = number,
            )
        }
        return circle!!
    }

    override suspend fun join(inviteCode: String): Circle {
        val circle = requireNotNull(state.value.circles.values.firstOrNull { it.inviteCode == inviteCode }) {
            "no circle has the invite code $inviteCode"
        }
        state.update { current ->
            if (circle.id in current.myCircleIds) current else current.copy(myCircleIds = current.myCircleIds + circle.id)
        }
        return circle
    }

    override suspend fun leave(circleId: String) {
        state.update { current ->
            current.copy(
                myCircleIds = current.myCircleIds - circleId,
                // Leaving withdraws what the user already published there.
                published = current.published.filterKeys { (id, _) -> id != circleId },
            )
        }
    }

    override suspend fun publishDay(circleId: String, day: LocalDate, stats: DayStats) {
        // Keyed by day, so publishing a day again replaces it rather than counting it twice.
        state.update { current -> current.copy(published = current.published + ((circleId to day) to stats)) }
    }

    private fun weekOf(current: State, circleId: String, weekStart: LocalDate): CircleWeek? {
        val circle = current.circles[circleId] ?: return null
        // Like a real cache, it has nothing for weeks before it first fetched.
        if (weekStart < seededWeek) return null

        val isSeededWeek = weekStart == seededWeek
        val others = if (circleId == FAJR.id) {
            FAJR_OTHERS.map { (member, points) ->
                if (isSeededWeek) {
                    MemberWeek(member, isMe = false, points = points, maxPoints = SEEDED_MAX)
                } else {
                    MemberWeek(member, isMe = false, points = 0, maxPoints = 0)
                }
            }
        } else {
            emptyList()
        }

        val me = myWeek(current, circleId, weekStart, isSeededWeek)
        // The seed order is deliberately not ranked; the user sits in the middle of it.
        val members = others.take(1) + me + others.drop(1)

        return CircleWeek(
            circle = circle,
            weekStart = weekStart,
            members = members,
            groupStreak = if (circleId == FAJR.id) FAJR_GROUP_STREAK else 0,
            refreshedAt = refreshedAt,
        )
    }

    private fun myWeek(current: State, circleId: String, weekStart: LocalDate, isSeededWeek: Boolean): MemberWeek {
        val weekDays = List(DAYS_IN_WEEK) { weekStart.plusDays(it) }.toSet()
        val mine = current.published.filterKeys { (id, day) -> id == circleId && day in weekDays }.values

        val (points, maxPoints) = when {
            mine.isNotEmpty() -> mine.sumOf { it.points } to mine.sumOf { it.maxPoints }
            circleId == FAJR.id && isSeededWeek -> SEEDED_ME to SEEDED_MAX
            else -> 0 to 0
        }
        return MemberWeek(ME, isMe = true, points = points, maxPoints = maxPoints)
    }

    private data class State(
        val circles: Map<String, Circle>,
        val myCircleIds: List<String>,
        val published: Map<Pair<String, LocalDate>, DayStats>,
        val created: Int,
    )

    private companion object {
        const val DAYS_IN_WEEK = 7
        const val SEEDED_MAX = 90
        const val SEEDED_ME = 70
        const val FAJR_GROUP_STREAK = 4

        val FAJR = Circle(id = "fajr-circle", name = "حلقة الفجر", inviteCode = "FAJR-4217", memberCount = 5)

        // The user has no display name of their own here: a screen labels its own row, and
        // what other members see of the user is decided by the privacy setting.
        val ME = CircleMember(id = "me", displayName = null)

        val FAJR_OTHERS = listOf(
            CircleMember(id = "khalid", displayName = "خالد") to 64,
            CircleMember(id = "salim", displayName = "سليم") to 83,
            CircleMember(id = "muadh", displayName = "معاذ") to 58,
            CircleMember(id = "yasir", displayName = "ياسر") to 79,
        )
    }
}
