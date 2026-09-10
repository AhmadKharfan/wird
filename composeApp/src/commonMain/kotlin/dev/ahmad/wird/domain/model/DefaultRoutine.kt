package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate

/**
 * The routine a fresh install starts with: fifteen points a day.
 *
 * The five prayers are the one counter, worth five; the other ten are worth a point each.
 * The order is the order they appear on the Today screen — `sortOrder` runs 0 to 10 and
 * [DaySnapshot] sorts by it, so the list here is the single place that decides it.
 *
 * These are the only Arabic strings in the domain, and they are names rather than display
 * text: they are what the user renames, not what a screen formats.
 */
object DefaultRoutine {

    /**
     * The routine as it would be created on [day].
     *
     * Every habit starts on [day] rather than at some earlier epoch, so a fresh install
     * is never shown a missed day it did not have.
     */
    fun habitsFrom(day: LocalDate): List<Habit> = listOf(
        habit(day, 0, "five-prayers", "الصلوات الخمس", HabitKind.COUNTER, target = 5),
        habit(day, 1, "duha", "صلاة الضحى"),
        habit(day, 2, "witr", "صلاة الوتر"),
        habit(day, 3, "rawatib", "سنن الرواتب"),
        habit(day, 4, "quran-juz", "جزء قرآن"),
        habit(day, 5, "adhkar-morning", "أذكار الصباح"),
        habit(day, 6, "adhkar-evening", "أذكار المساء"),
        habit(day, 7, "adhkar-sleep", "أذكار النوم"),
        habit(day, 8, "adhkar-waking", "أذكار الاستيقاظ"),
        habit(day, 9, "surah-mulk", "سورة الملك"),
        habit(day, 10, "salawat-hundred", "مئة صلاة على النبي ﷺ"),
    )

    private fun habit(
        day: LocalDate,
        sortOrder: Int,
        id: String,
        name: String,
        kind: HabitKind = HabitKind.BOOL,
        target: Int = 1,
    ) = Habit(
        id = id,
        name = name,
        kind = kind,
        target = target,
        iconKey = id,
        sortOrder = sortOrder,
        effectiveFrom = day,
    )
}
