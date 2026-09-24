package com.example.master

import io.mpos.transactions.Currency
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class PaymentRequestsTest {
    @Test fun parsesDecimalWithoutFloatingPointRounding() {
        assertEquals(BigDecimal("12.34"), PaymentRequests.amount("12.34", Currency.EUR))
        assertEquals(BigDecimal("1.00"), PaymentRequests.amount(" 1 ", Currency.EUR))
    }

    @Test fun rejectsInvalidOrNonPositiveAmounts() {
        listOf("", "0", "-1", "1e3", "NaN", "1,25", ".5", "1.001").forEach { input ->
            assertThrows(IllegalArgumentException::class.java) { PaymentRequests.amount(input, Currency.EUR) }
        }
    }

    @Test fun respectsCurrencyPrecision() {
        assertEquals(BigDecimal("100"), PaymentRequests.amount("100", Currency.JPY))
        assertThrows(IllegalArgumentException::class.java) { PaymentRequests.amount("100.50", Currency.JPY) }
    }

    @Test fun normalizesCurrencyAndRejectsUnknownCodes() {
        assertEquals(Currency.EUR, PaymentRequests.currency(" eur "))
        assertThrows(IllegalArgumentException::class.java) { PaymentRequests.currency("XYZ") }
    }

    @Test fun requiresReferencesForFinancialOperations() {
        assertThrows(IllegalArgumentException::class.java) { PaymentRequests.sale("1", "EUR", " ") }
        assertThrows(IllegalArgumentException::class.java) { PaymentRequests.refund(" ", "", "EUR") }
    }

    @Test fun buildsSaleAndBothRefundTypes() {
        assertNotNull(PaymentRequests.sale("1.25", "EUR", "order-123"))
        assertNotNull(PaymentRequests.refund("original-id", "", ""))
        assertNotNull(PaymentRequests.refund("original-id", "0.50", "EUR"))
    }
}
