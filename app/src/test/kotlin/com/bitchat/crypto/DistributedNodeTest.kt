package com.bitchat.crypto

import org.junit.Assert.assertTrue
import org.junit.Test

class DistributedNodeTest {
    @Test
    fun `nodes converge after gossip transfer`() {
        NetworkService.resetForTests()
        val n1 = DistributedNode("n1")
        val n2 = DistributedNode("n2")
        n1.attach()
        n2.attach()

        n1.deposit("alice", 200.0)
        n2.deposit("alice", 200.0)

        n1.submitTransfer("alice", "bob", 50.0)

        // both should have applied the same transfer eventually in this synchronous simulation
        assertTrue(n1.balance("bob") > 0.0)
        assertTrue(n2.balance("bob") > 0.0)
    }
}
