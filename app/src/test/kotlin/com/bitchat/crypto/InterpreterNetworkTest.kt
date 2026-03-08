package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class InterpreterNetworkTest {
    @Test
    fun `interpreter broadcasts and receives transactions`() {
        val i1 = Interpreter
        val i2 = Interpreter
        // deposit directly since tests need to set state
        i1.deposit("alice", 100.0)
        i2.deposit("alice", 100.0)

        // now perform a transfer on i1
        i1.execute(Interpreter.CryptoTransaction("alice:bob:50".toByteArray()))

        // due to network broadcast, i2 should also apply same transfer
        val balAlice = i2.getBalance("alice")
        val balBob = i2.getBalance("bob")
        assertEquals(50.0, balAlice, 1e-6) // 100 - 50 taxed
        assertEquals(47.5, balBob, 1e-6)  // 50 - 5% tax
    }
}
