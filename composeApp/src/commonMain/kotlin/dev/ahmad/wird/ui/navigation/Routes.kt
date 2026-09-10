package dev.ahmad.wird.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe routes. One object or data class per destination. */
@Serializable
data object TodayRoute

@Serializable
data object HistoryRoute

@Serializable
data object SettingsRoute
