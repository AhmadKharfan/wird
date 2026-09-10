package dev.ahmad.wird.ui.feature.today

import dev.ahmad.wird.domain.model.Prayer
import dev.ahmad.wird.ui.base.BaseInteractionListener

/** The screen's whole interaction surface, in one place. */
interface TodayInteractionListener : BaseInteractionListener {
    fun onPrayerTapped(prayer: Prayer)
    fun onRetry()
}
