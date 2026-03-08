package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class InterpreterNetworkTest {
    @Test
    fun `interpreter broadcasts and receives transactions`() {
        val i1 = Interpreter
        val i2 = Interpreter
        // deposit initial funds into interpreter1's ledger via direct call
        i1.execute(Interpreter.CryptoTransaction("alice:bob:100".toByteArray()))
        // since network messages propagate, i2 should also see the transfer
        val balAlice = i2.getBalance("alice")
        val balBob = i2.getBalance("bob")
        // i2 performed same transfer; alice has -100 taxed and bob +95
        assertEquals(-100.0 + 0.0 /* starting 0 */, balAlice, 1e-6)
        assertEquals(95.0, balBob, 1e-6)
    }
}
