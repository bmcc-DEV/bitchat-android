package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class InterpreterNetworkTest {
    @Test
    fun `interpreter broadcasts and receives transactions`() {
        Interpreter.resetState()
        Interpreter.deposit("alice", 100.0)
        Interpreter.execute(Interpreter.CryptoTransaction("alice:bob:50".toByteArray()))
        assertEquals(50.0, Interpreter.getBalance("alice"), 1e-6)
        assertEquals(47.5, Interpreter.getBalance("bob"), 1e-6)
    }
}
