package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate

/** Whether a prayer was performed, and how, on one day. */
data class PrayerRecord(
    val date: LocalDate,
    val prayer: Prayer,
    val status: PrayerStatus,
)

enum class PrayerStatus {
    NOT_RECORDED,
    ON_TIME,
    LATE,
    MISSED,
}
