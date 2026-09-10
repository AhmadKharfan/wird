package dev.ahmad.wird.domain.repository

import dev.ahmad.wird.domain.model.Circle
import dev.ahmad.wird.domain.model.CircleStanding
import dev.ahmad.wird.domain.model.DayStats
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Circles are the one part of the product that is not local-only. **Nothing implements
 * this yet** — it is declared now so the shape is fixed before a backend exists, and so
 * the use cases above it can be written against something stable.
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
 * Unlike the local repositories, these calls may await the network. No UI interaction is
 * allowed to block on one: a circle screen shows what the flow has cached and refreshes
 * underneath.
 */
interface CircleRepository {

    /** The circles the user belongs to. */
    fun observeMyCircles(): Flow<List<Circle>>

    /** How each member of [circleId] scored on [day], best first. */
    fun observeStandings(circleId: String, day: LocalDate): Flow<List<CircleStanding>>

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
