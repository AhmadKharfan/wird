package dev.ahmad.wird.ui.feature.today

import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import dev.ahmad.wird.ui.format.NumeralFormatter

/** UI wording lives in ui/. The domain carries no display text. */
fun Habit.valueLabel(value: Int, numerals: NumeralFormatter): String = when (kind) {
    HabitKind.BOOL -> if (value > 0) "تم" else "لم يتم"
    HabitKind.COUNTER -> "${numerals.format(value)} / ${numerals.format(target)}"
}

/** The day so far, such as "3 / 15", in the chosen digits. */
fun scoreLabel(points: Int, maxPoints: Int, numerals: NumeralFormatter): String =
    "${numerals.format(points)} / ${numerals.format(maxPoints)}"

const val FAILED_TO_LOAD = "تعذّر تحميل اليوم"
const val FAILED_TO_SAVE = "تعذّر حفظ التسجيل"
const val RETRY = "إعادة المحاولة"
