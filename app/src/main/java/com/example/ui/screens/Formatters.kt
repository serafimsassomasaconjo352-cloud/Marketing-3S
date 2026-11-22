package com.example.ui.screens

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {
    private val ptAo = Locale.forLanguageTag("pt-AO")
    private val symbols = DecimalFormatSymbols(ptAo).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val currencyFormat = DecimalFormat("#,##0.00", symbols)
    private val compactFormat = DecimalFormat("#,##0", symbols)
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", ptAo)

    fun formatKz(value: Double, compact: Boolean = false): String {
        val formatter = if (compact && abs(value) >= 1000.0) compactFormat else currencyFormat
        return "${formatter.format(value)} Kz"
    }

    fun formatPercent(value: Double): String {
        return "${"%.1f".format(ptAo, value)}%"
    }

    fun formatDate(millis: Long): String {
        return dateFormat.format(Date(millis))
    }

    fun parsePositiveKz(input: String): Double? {
        val cleaned = input.trim()
            .replace("Kz", "", ignoreCase = true)
            .replace(" ", "")
        val normalized = if (cleaned.contains(",") && cleaned.contains(".")) {
            cleaned.replace(".", "").replace(",", ".")
        } else if (cleaned.contains(",")) {
            cleaned.replace(",", ".")
        } else {
            cleaned
        }
        return normalized.toDoubleOrNull()?.takeIf { it > 0.0 && it <= 1_000_000_000_000.0 }
    }

    fun parseNonNegativeKz(input: String): Double? {
        if (input.isBlank()) return 0.0
        val cleaned = input.trim()
            .replace("Kz", "", ignoreCase = true)
            .replace(" ", "")
        val normalized = if (cleaned.contains(",") && cleaned.contains(".")) {
            cleaned.replace(".", "").replace(",", ".")
        } else if (cleaned.contains(",")) {
            cleaned.replace(",", ".")
        } else {
            cleaned
        }
        return normalized.toDoubleOrNull()?.takeIf { it >= 0.0 && it <= 1_000_000_000_000.0 }
    }
}
