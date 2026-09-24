package com.example.master

import io.mpos.transactions.Currency
import io.mpos.transactions.parameters.TransactionParameters
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object PaymentRequests {
    fun currency(value: String): Currency = try {
        Currency.valueOf(value.trim().uppercase(Locale.ROOT))
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("Enter a currency supported by the payment SDK.")
    }

    fun amount(value: String, currency: Currency): BigDecimal {
        val text = value.trim()
        require(Regex("[0-9]+(\\.[0-9]+)?").matches(text)) { "Enter a positive decimal amount, such as 1.00." }
        val amount = text.toBigDecimal()
        require(amount.signum() > 0) { "Amount must be greater than zero." }
        val digits = java.util.Currency.getInstance(currency.name).defaultFractionDigits
        require(digits >= 0) { "Unsupported currency precision." }
        return try { amount.setScale(digits, RoundingMode.UNNECESSARY) }
        catch (_: ArithmeticException) { throw IllegalArgumentException("Amount has too many decimal places for ${currency.name}.") }
    }

    fun sale(value: String, code: String, reference: String): TransactionParameters {
        val currency = currency(code)
        require(reference.isNotBlank()) { "Enter a sale reference." }
        return TransactionParameters.Builder().charge(amount(value, currency), currency)
            .customIdentifier(reference.trim()).build()
    }

    fun refund(id: String, value: String, code: String): TransactionParameters {
        require(id.isNotBlank()) { "Enter the original transaction ID." }
        val builder = TransactionParameters.Builder().refund(id.trim())
        if (value.isNotBlank()) {
            val currency = currency(code)
            builder.amountAndCurrency(amount(value, currency), currency)
        }
        return builder.build()
    }
}
