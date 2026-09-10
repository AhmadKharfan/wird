package dev.ahmad.wird.ui.feature.today

import dev.ahmad.wird.domain.model.Prayer
import kotlinx.datetime.LocalDateTime

/** UI wording lives in ui/. The domain enum carries no display text. */
fun Prayer.arabicLabel(): String = when (this) {
    Prayer.FAJR -> "الفجر"
    Prayer.DHUHR -> "الظهر"
    Prayer.ASR -> "العصر"
    Prayer.MAGHRIB -> "المغرب"
    Prayer.ISHA -> "العشاء"
}

fun LocalDateTime.timeLabel(): String {
    val h = hour.toString().padStart(2, '0')
    val m = minute.toString().padStart(2, '0')
    return "$h:$m"
}
