package dev.ahmad.wird.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Storage shape for the single settings row. Never leaves `data/`.
 *
 * The primary key is pinned to [SINGLETON_ID] so an upsert always replaces rather than
 * accumulating — there is exactly one settings row, and the schema is what guarantees it
 * rather than a convention every caller has to remember.
 *
 * Location is stored as two nullable columns rather than an embedded type: it is absent
 * until the user provides one, and a half-set coordinate is not representable.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val themeMode: String,
    val numeralSystem: String,
    val latitude: Double?,
    val longitude: Double?,
    val calculationMethod: String,
    val privacyMode: String,
    // Remembered separately from privacyMode, so it survives a mode that does not show it.
    val nickname: String? = null,
    // The default is declared so the schema matches the column the 4 -> 5 migration adds.
    @ColumnInfo(defaultValue = "0") val onboardingCompleted: Boolean = false,
    val updatedAt: Long,
) {
    companion object {
        /** The only row id this table ever holds. */
        const val SINGLETON_ID: Int = 0
    }
}
