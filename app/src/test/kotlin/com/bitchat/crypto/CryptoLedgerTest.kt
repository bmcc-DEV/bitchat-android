package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CryptoLedgerTest {
    @Test
    fun `deposit increases balance`() {
        val ledger = CryptoLedger()
        ledger.deposit("alice", 100.0)
        assertEquals(100.0, ledger.getBalance("alice"), 1e-6)
    }

    @Test
    fun `transfer applies tax and moves funds`() {
        val ledger = CryptoLedger(taxRate = 0.1)
        ledger.deposit("alice", 200.0)
        val success = ledger.transfer("alice", "bob", 50.0)
        assertEquals(true, success)
        // alice loses 50.0
        assertEquals(150.0, ledger.getBalance("alice"), 1e-6)
        // bob receives 45.0 after 10% tax (50 - 5)
        assertEquals(45.0, ledger.getBalance("bob"), 1e-6)
    }

    @Test
    fun `transfer fails with insufficient funds`() {
        val ledger = CryptoLedger()
        ledger.deposit("alice", 10.0)
        val success = ledger.transfer("alice", "bob", 20.0)
        assertFalse(success)
    }
}
