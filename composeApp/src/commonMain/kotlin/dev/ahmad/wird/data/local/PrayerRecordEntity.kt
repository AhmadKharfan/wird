package dev.ahmad.wird.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Storage shape. Never leaves data/ — map to a domain model at the boundary.
 * Primary key is "<epochDay>:<prayer>" so a re-record replaces in place.
 */
@Entity(tableName = "prayer_record")
data class PrayerRecordEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val prayer: String,
    val status: String,
)
