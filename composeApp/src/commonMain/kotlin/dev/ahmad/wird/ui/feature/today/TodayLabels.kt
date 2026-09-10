package dev.ahmad.wird.ui.feature.today

import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind

/** UI wording lives in ui/. The domain carries no display text. */
fun Habit.valueLabel(value: Int): String = when (kind) {
    HabitKind.BOOL -> if (value > 0) "تم" else "لم يتم"
    HabitKind.COUNTER -> "$value / $target"
}

const val FAILED_TO_LOAD = "تعذّر تحميل اليوم"
const val FAILED_TO_SAVE = "تعذّر حفظ التسجيل"
const val RETRY = "إعادة المحاولة"
