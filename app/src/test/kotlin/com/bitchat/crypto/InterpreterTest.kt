package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class InterpreterTest {
    @Test
    fun `interpreter transfers funds according to payload`() {
        Interpreter.resetState()
        Interpreter.deposit("alice", 100.0)
        val tx = Interpreter.CryptoTransaction("alice:bob:100".toByteArray())
        Interpreter.execute(tx)
        assertEquals(0.0, Interpreter.getBalance("alice"), 1e-6)
        assertEquals(95.0, Interpreter.getBalance("bob"), 1e-6) // default 5% tax
    }
}
