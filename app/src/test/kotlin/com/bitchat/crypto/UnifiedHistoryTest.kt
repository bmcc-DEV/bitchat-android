package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class UnifiedHistoryTest {
    @Test
    fun `ledger records unified value history`() {
        val ledger = CryptoLedger()
        ledger.deposit("a", 100.0)
        ledger.deposit("b", 100.0)
        ledger.transfer("a", "b", 50.0)
        val history = ledger.getUnifiedHistory()
        // should have one entry
        assertEquals(1, history.size)
        // unified value should equal sum of contribs+tax computed above
        val taxed = 50.0 * 0.95
        val tax = 50.0 * 0.05
        val expected = UnifiedValueCalculator.computeUnifiedValue(
            listOf(taxed, tax, tax * 0.5, tax * 2.0),
            tax
        )
        assertEquals(expected, history[0], 1e-6)
    }
}
