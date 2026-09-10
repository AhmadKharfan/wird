package dev.ahmad.wird.ui.format

import dev.ahmad.wird.domain.model.NumeralSystem
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every number the app shows goes through this one formatter, so the numeral setting
 * switches all of them at once and no screen can forget to.
 *
 * Expected values are written out by hand. Arabic-Indic digits are U+0660 to U+0669; the
 * look-alike Extended Arabic-Indic block at U+06F0 is the Persian and Urdu set, and using it
 * by mistake renders four, five and six in shapes an Arabic reader does not expect.
 */
class NumeralFormatterTest {

    private val western = NumeralFormatter(NumeralSystem.WESTERN)
    private val arabicIndic = NumeralFormatter(NumeralSystem.ARABIC_INDIC)

    // --- numbers ----------------------------------------------------------------------

    @Test
    fun writesWesternDigits() {
        assertEquals("1234", western.format(1234))
    }

    @Test
    fun writesArabicIndicDigits() {
        assertEquals("١٢٣٤", arabicIndic.format(1234))
    }

    @Test
    fun writesZero() {
        assertEquals("٠", arabicIndic.format(0))
    }

    @Test
    fun keepsTheMinusSign() {
        assertEquals("-٥", arabicIndic.format(-5))
    }

    @Test
    fun mapsEveryDigitToTheArabicIndicBlock() {
        assertEquals("٠١٢٣٤٥٦٧٨٩", arabicIndic.localize("0123456789"))
    }

    @Test
    fun usesTheArabicIndicBlockRatherThanThePersianOne() {
        // U+0664 is the Arabic four; U+06F4 is the Persian one, and they are drawn differently.
        assertEquals(0x0664, arabicIndic.format(4).single().code)
    }

    // --- digits inside text that was already put together ---------------------------------

    @Test
    fun localizesTheDigitsOfAScore() {
        assertEquals("٣ / ١٥", arabicIndic.localize("3 / 15"))
    }

    @Test
    fun localizesTheDigitsOfATime() {
        assertEquals("٠٤:٥٢", arabicIndic.localize("04:52"))
    }

    @Test
    fun leavesTextWithNoDigitsAlone() {
        assertEquals("الصلوات الخمس", arabicIndic.localize("الصلوات الخمس"))
    }

    @Test
    fun leavesTextUntouchedWhenWesternDigitsAreChosen() {
        // Western is the identity: it never rewrites digits the user typed themselves, such as
        // Arabic-Indic digits inside a nickname.
        assertEquals("أبو ٣", western.localize("أبو ٣"))
        assertEquals("3 / 15", western.localize("3 / 15"))
    }
}
