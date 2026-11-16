package com.swearjar.domain.rules

import com.swearjar.domain.model.Escalation
import com.swearjar.domain.model.JarSettings
import com.swearjar.domain.model.PricingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PricingRulesTest {

    private fun settings(mode: PricingMode, escalation: Escalation = Escalation()) =
        JarSettings(pricingMode = mode, escalation = escalation)

    @Test
    fun `fixed ignores overrides and history`() {
        assertEquals(50, PricingRules.priceFor(settings(PricingMode.FIXED), 50, priorOffences = 20, overrideMinor = 999))
    }

    @Test
    fun `override replaces the price when given`() {
        val s = settings(PricingMode.FIXED_WITH_OVERRIDE)
        assertEquals(50, PricingRules.priceFor(s, 50))
        assertEquals(300, PricingRules.priceFor(s, 50, overrideMinor = 300))
        assertEquals(0, PricingRules.priceFor(s, 50, overrideMinor = 0))
    }

    @Test
    fun `negative override is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            PricingRules.priceFor(settings(PricingMode.FIXED_WITH_OVERRIDE), 50, overrideMinor = -1)
        }
    }

    @Test
    fun `escalating doubles every three offences up to the cap`() {
        val s = settings(PricingMode.ESCALATING)
        val prices = (0..12).map { PricingRules.priceFor(s, 50, priorOffences = it) }
        assertEquals(listOf<Long>(50, 50, 50, 100, 100, 100, 200, 200, 200, 400, 400, 400, 400), prices)
    }

    @Test
    fun `escalating honours a custom multiplier and step`() {
        val s = settings(PricingMode.ESCALATING, Escalation(every = 1, multiplierPercent = 150, maxSteps = 2))
        assertEquals(100, PricingRules.priceFor(s, 100, priorOffences = 0))
        assertEquals(150, PricingRules.priceFor(s, 100, priorOffences = 1))
        assertEquals(225, PricingRules.priceFor(s, 100, priorOffences = 2))
        assertEquals(225, PricingRules.priceFor(s, 100, priorOffences = 9))
    }

    @Test
    fun `no entry can exceed the ceiling`() {
        val runaway = settings(PricingMode.ESCALATING, Escalation(every = 1, multiplierPercent = 1000, maxSteps = 50))
        assertEquals(PricingRules.MAX_ENTRY_MINOR, PricingRules.priceFor(runaway, 500, priorOffences = 50))
        assertEquals(
            PricingRules.MAX_ENTRY_MINOR,
            PricingRules.priceFor(settings(PricingMode.FIXED_WITH_OVERRIDE), 50, overrideMinor = Long.MAX_VALUE),
        )
    }
}
