package dev.ahmad.wird.ui.format

import dev.ahmad.wird.domain.model.NumeralSystem

/**
 * The single formatter every number in the app goes through, so the numeral setting switches
 * all of them at once and no screen can forget to.
 *
 * Arabic-Indic digits are the block from U+0660 to U+0669. The look-alike block at U+06F0 is
 * the Extended Arabic-Indic set used for Persian and Urdu; it draws four, five and six in
 * shapes an Arabic reader does not expect, so it is never used here.
 *
 * Western is the identity. It never rewrites digits a user typed themselves, such as
 * Arabic-Indic digits inside a nickname.
 */
class NumeralFormatter(private val system: NumeralSystem) {

    /** The number in the chosen digits. */
    fun format(value: Int): String = localize(value.toString())

    /** The ASCII digits of text that was already put together, such as a score or a time. */
    fun localize(text: String): String = when (system) {
        NumeralSystem.WESTERN -> text
        NumeralSystem.ARABIC_INDIC -> buildString(text.length) {
            text.forEach { char -> append(char.toArabicIndic()) }
        }
    }

    private fun Char.toArabicIndic(): Char =
        if (code in ASCII_ZERO..ASCII_NINE) Char(ARABIC_INDIC_ZERO + (code - ASCII_ZERO)) else this

    private companion object {
        const val ASCII_ZERO = 0x30
        const val ASCII_NINE = 0x39
        const val ARABIC_INDIC_ZERO = 0x0660
    }
}
