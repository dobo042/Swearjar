package com.swearjar.domain.rules

import com.swearjar.domain.model.Escalation
import com.swearjar.domain.model.JarSettings
import com.swearjar.domain.model.Minor
import com.swearjar.domain.model.PricingMode

/** Interprets a jar's [PricingMode]. */
object PricingRules {

    /** Upper bound on any single entry, so a typo or runaway multiplier cannot wreck a ledger. */
    const val MAX_ENTRY_MINOR: Minor = 1_000_000

    /**
     * The amount to charge for one offence.
     *
     * @param basePriceMinor the topic's price
     * @param priorOffences how many live entries the offender already has on this topic within
     *   the escalation window; ignored unless the mode is [PricingMode.ESCALATING]
     * @param overrideMinor amount typed in by whoever is logging; ignored unless the mode is
     *   [PricingMode.FIXED_WITH_OVERRIDE]
     */
    fun priceFor(
        settings: JarSettings,
        basePriceMinor: Minor,
        priorOffences: Int = 0,
        overrideMinor: Minor? = null,
    ): Minor {
        require(basePriceMinor >= 0) { "price must not be negative" }
        val price = when (settings.pricingMode) {
            PricingMode.FIXED -> basePriceMinor
            PricingMode.FIXED_WITH_OVERRIDE -> overrideMinor ?: basePriceMinor
            PricingMode.ESCALATING -> escalate(settings.escalation, basePriceMinor, priorOffences)
        }
        require(price >= 0) { "price must not be negative" }
        return price.coerceAtMost(MAX_ENTRY_MINOR)
    }

    /** How many times the multiplier applies for someone with [priorOffences] already. */
    fun escalationSteps(escalation: Escalation, priorOffences: Int): Int =
        (priorOffences.coerceAtLeast(0) / escalation.every).coerceAtMost(escalation.maxSteps)

    private fun escalate(escalation: Escalation, basePriceMinor: Minor, priorOffences: Int): Minor {
        var price = basePriceMinor
        repeat(escalationSteps(escalation, priorOffences)) {
            price = price * escalation.multiplierPercent / 100
            if (price >= MAX_ENTRY_MINOR) return MAX_ENTRY_MINOR
        }
        return price
    }
}
