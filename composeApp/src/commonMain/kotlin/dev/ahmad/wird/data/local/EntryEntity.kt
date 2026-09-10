package dev.ahmad.wird.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Storage shape for what was recorded against one habit on one day. Never leaves `data/`.
 *
 * The primary key is a **client-generated UUID** rather than a natural
 * `habitId:day` composite. That is deliberate and load-bearing for sync: a row keeps one
 * identity for its whole life, so a device can create rows offline, and the server can
 * accept them, without either side inventing a key or renaming a row later. The natural
 * key survives as a unique index on `(habitId, epochDay)`, which is what actually enforces
 * one entry per habit per day.
 *
 * [epochDay] is the user's local calendar day as recorded — never re-derived from
 * [updatedAt] — so moving between timezones cannot shift an entry onto a neighbouring day.
 * [updatedAt] is epoch milliseconds, stamped on every write for last-write-wins.
 */
@Entity(
    tableName = "entry",
    indices = [
        Index(value = ["habitId", "epochDay"], unique = true),
        Index(value = ["epochDay"]),
    ],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val epochDay: Long,
    val value: Int,
    val updatedAt: Long,
)
