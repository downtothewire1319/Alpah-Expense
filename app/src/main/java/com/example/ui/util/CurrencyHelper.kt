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

    fun format(amount: Double, symbol: String = currentCurrency.symbol): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return "$symbol${formatter.format(amount)}"
    }
}
