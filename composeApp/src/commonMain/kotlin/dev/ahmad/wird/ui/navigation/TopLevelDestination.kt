package dev.ahmad.wird.ui.navigation

/**
 * The bottom bar's destinations, in display order. Labels live here rather than in the
 * bar so the bar stays a dumb renderer.
 */
enum class TopLevelDestination(val label: String) {
    TODAY("اليوم"),
    HISTORY("السجل"),
    SETTINGS("الإعدادات"),
    ;

    val route: Any
        get() = when (this) {
            TODAY -> TodayRoute
            HISTORY -> HistoryRoute
            SETTINGS -> SettingsRoute
        }
}
