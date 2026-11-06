package com.example.ui.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyHelper {
    data class Currency(val code: String, val symbol: String, val name: String)

    val supportedCurrencies = listOf(
        Currency("BDT", "৳", "Bangladeshi Taka (BDT)"),
        Currency("USD", "$", "US Dollar (USD)"),
        Currency("EUR", "€", "Euro (EUR)"),
        Currency("GBP", "£", "British Pound (GBP)"),
        Currency("INR", "₹", "Indian Rupee (INR)")
    )

    var currentCurrency: Currency = supportedCurrencies[0]

    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun format(amount: Double, symbol: String = currentCurrency.symbol): String {
        return formatInternal(amount, symbol, useCommas = true)
    }

    fun formatNoComma(amount: Double, symbol: String = currentCurrency.symbol): String {
        return formatInternal(amount, symbol, useCommas = true)
    }

    private fun formatInternal(amount: Double, symbol: String, useCommas: Boolean): String {
        val absAmount = kotlin.math.abs(amount).toLong()
        val amountStr = if (useCommas) {
            formatSouthAsian(absAmount)
        } else {
            absAmount.toString()
        }
        
        val bengaliAmount = amountStr.map { char ->
            if (char in '0'..'9') bengaliDigits[char - '0'] else char
        }.joinToString("")
        
        return "$symbol$bengaliAmount"
    }

    /**
     * Formats a number with South Asian grouping (Lakh, Crore).
     * Example: 1500000 -> 15,00,000
     */
    private fun formatSouthAsian(number: Long): String {
        val str = number.toString()
        if (str.length <= 3) return str
        
        val lastThree = str.substring(str.length - 3)
        var remaining = str.substring(0, str.length - 3)
        
        val result = StringBuilder()
        while (remaining.length > 2) {
            result.insert(0, "," + remaining.substring(remaining.length - 2))
            remaining = remaining.substring(0, remaining.length - 2)
        }
        
        if (remaining.isNotEmpty()) {
            result.insert(0, remaining)
        } else if (result.startsWith(",")) {
            result.deleteCharAt(0)
        }
        
        return result.toString() + "," + lastThree
    }
}
