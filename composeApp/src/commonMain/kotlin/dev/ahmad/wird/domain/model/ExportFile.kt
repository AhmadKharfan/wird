package dev.ahmad.wird.domain.model

/**
 * A finished export, ready to hand to the platform: saved as a download in the browser, or
 * offered through the share sheet on Android.
 */
data class ExportFile(
    val name: String,
    val contents: String,
)
