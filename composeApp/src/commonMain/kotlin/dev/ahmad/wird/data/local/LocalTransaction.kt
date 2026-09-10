package dev.ahmad.wird.data.local

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection

/**
 * Runs a block of local writes as one transaction: every row it touches and every outbox
 * record it appends land together, or none of them do.
 *
 * A repository write is never one statement — the row and its outbox record, often with a
 * read before them — so without this a failure part-way leaves the data and the queue
 * disagreeing, and two writes that interleave can each act on the same old value.
 */
interface LocalTransaction {
    suspend fun <R> write(block: suspend () -> R): R
}

/**
 * [LocalTransaction] on the Room database: an immediate transaction on the writer connection,
 * which also makes concurrent writers take turns, so a read-modify-write cannot lose an update.
 */
class RoomLocalTransaction(private val database: WirdDatabase) : LocalTransaction {
    override suspend fun <R> write(block: suspend () -> R): R =
        database.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
}
