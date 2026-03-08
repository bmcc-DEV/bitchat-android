package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class NodeTest {
    @Test
    fun `nodes sync balances after transfer`() {
        val n1 = Node("n1")
        val n2 = Node("n2")
        n1.deposit("alice", 100.0)
        n2.deposit("alice", 50.0)
        // perform transfer on n1; broadcast should propagate
        n1.transfer("alice", "bob", 20.0)
        // give time for network (immediate in this stub)
        Thread.sleep(100)
        // balances should average for alice: (80+50)/2=65
        assertEquals(65.0, n2.getBalance("alice"), 1e-6)
    }
}
