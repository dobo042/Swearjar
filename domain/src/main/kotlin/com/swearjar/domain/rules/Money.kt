package com.swearjar.domain.rules

import com.swearjar.domain.model.Minor
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object Money {

    /** e.g. `format(250, "GBP", Locale.UK)` is "£2.50". */
    fun format(amountMinor: Minor, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val currency = Currency.getInstance(currencyCode)
        val digits = currency.defaultFractionDigits.coerceAtLeast(0)
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
        return formatter.format(BigDecimal.valueOf(amountMinor, digits))
    }

    /**
     * Reads an amount typed as major units ("2.50", "2,50", "3") into minor units. Returns
     * null for anything that is not a non-negative amount with a sensible number of decimals.
     */
    fun parse(input: String, currencyCode: String): Minor? {
        val digits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
        val text = input.trim().replace(',', '.')
        if (!Regex("""\d+(\.\d+)?""").matches(text)) return null
        val value = text.toBigDecimalOrNull() ?: return null
        if (value.scale() > digits) return null
        return runCatching { value.movePointRight(digits).longValueExact() }.getOrNull()
    }
}
