package dev.ahmad.wird.domain.repository

import dev.ahmad.wird.domain.model.Circle
import dev.ahmad.wird.domain.model.CircleWeek
import dev.ahmad.wird.domain.model.DayStats
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Circles are the one part of the product that is not local-only. The backend lands in a
 * later pack; until then a seeded stand-in implements this, and nothing above the interface
 * can tell the difference.
 *
 * The signatures are chosen so a networked implementation drops in without changing any
 * of them:
 *
 * - **Reads are [Flow], not `suspend`.** A remote implementation emits the cached value
 *   and then the refreshed one from the same flow. A `suspend fun getCircles()` would
 *   push polling onto every caller and would have to change shape the day caching lands.
 * - **Writes are `suspend` and return domain types.** No response envelope, no result
 *   wrapper, nothing named after a transport. Failure is an exception, exactly as it is
 *   for the local repositories, so callers handle both the same way.
 * - **Every id is an opaque [String]** the server is free to assign.
 * - **[publishDay] takes an already-computed [DayStats].** Deciding what may be shared is
 *   a domain decision driven by [dev.ahmad.wird.domain.model.PrivacyMode], and it is made
 *   before anything reaches this interface — a remote implementation is never in a
 *   position to over-share.
 *
 * The read side was redesigned before anything implemented it. A per-day standing could not
 * carry what the circle screens need — the Saturday week's totals, which member is the user,
 * the group streak, and when the copy was last fetched — so [observeWeek] replaced it.
 *
 * Unlike the local repositories, these calls may await the network. No UI interaction is
 * allowed to block on one: a circle screen shows what the flow has cached and refreshes
 * underneath.
 */
interface CircleRepository {

    /** The circles the user belongs to. */
    fun observeMyCircles(): Flow<List<Circle>>

    /**
     * The week of [circleId] that begins on the Saturday [weekStart], as last fetched, or
     * null when nothing has been fetched for it.
     *
     * A remote implementation emits its cached copy first and a refreshed one after, and a
     * network failure leaves the cached copy standing rather than failing the flow: circle
     * screens read offline from whatever was cached, with its [CircleWeek.refreshedAt] as
     * "last updated". Members arrive in no particular order — ranking is a domain decision.
     */
    fun observeWeek(circleId: String, weekStart: LocalDate): Flow<CircleWeek?>

    /** Creates a circle with the user as its first member, and returns it with its invite code. */
    suspend fun create(name: String): Circle

    /** Joins the circle behind [inviteCode] and returns it. */
    suspend fun join(inviteCode: String): Circle

    /** Leaves [circleId]. Standings the user already published are withdrawn. */
    suspend fun leave(circleId: String)

    /**
     * Shares [stats] for [day] with [circleId]. Callers pass only what
     * [dev.ahmad.wird.domain.model.PrivacyMode] permits.
     */
    suspend fun publishDay(circleId: String, day: LocalDate, stats: DayStats)
}
