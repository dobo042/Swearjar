package com.swearjar.domain.rules

import java.util.Locale
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodesAndMoneyTest {

    @Test
    fun `generated codes are well formed and round-trip through normalise`() {
        val random = Random(42)
        repeat(200) {
            val code = InviteCodes.generate(random)
            assertEquals(InviteCodes.LENGTH, code.length)
            assertTrue(code.all(InviteCodes.ALPHABET::contains))
            assertEquals(code, InviteCodes.normalise(code))
        }
    }

    @Test
    fun `normalise tidies typed input`() {
        assertEquals("ABC234", InviteCodes.normalise("  abc-234 "))
        assertEquals("ABC234", InviteCodes.normalise("abc 234"))
    }

    @Test
    fun `normalise rejects bad codes`() {
        assertNull(InviteCodes.normalise("ABC23"))
        assertNull(InviteCodes.normalise("ABC2345"))
        assertNull("O and 0 are not in the alphabet", InviteCodes.normalise("ABCO02"))
        assertNull(InviteCodes.normalise(""))
    }

    @Test
    fun `format uses the currency's own decimals`() {
        assertEquals("£2.50", Money.format(250, "GBP", Locale.UK))
        assertEquals("£0.05", Money.format(5, "GBP", Locale.UK))
        assertEquals("$1,234.00", Money.format(123400, "USD", Locale.US))
        assertEquals("¥500", Money.format(500, "JPY", Locale.JAPAN).replace('￥', '¥'))
    }

    @Test
    fun `parse reads typed amounts`() {
        assertEquals(250L, Money.parse("2.50", "GBP"))
        assertEquals(250L, Money.parse("2,5", "GBP"))
        assertEquals(300L, Money.parse(" 3 ", "GBP"))
        assertEquals(500L, Money.parse("500", "JPY"))
    }

    @Test
    fun `parse rejects nonsense`() {
        assertNull(Money.parse("", "GBP"))
        assertNull(Money.parse("-1", "GBP"))
        assertNull(Money.parse("abc", "GBP"))
        assertNull(Money.parse("1.234", "GBP"))
        assertNull(Money.parse("1.5", "JPY"))
        assertNull(Money.parse("1e5", "GBP"))
    }
}
