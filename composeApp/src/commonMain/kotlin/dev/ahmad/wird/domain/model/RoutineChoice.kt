package dev.ahmad.wird.domain.model

/** What a new user starts with, chosen once at the end of onboarding. */
enum class RoutineChoice {
    /** The eleven-habit, fifteen-point routine the app ships with. */
    DEFAULT_ROUTINE,

    /** Nothing at all; the user builds their own routine. */
    EMPTY,
}
