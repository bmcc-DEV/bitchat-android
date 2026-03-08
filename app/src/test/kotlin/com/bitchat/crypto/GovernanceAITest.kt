package com.bitchat.crypto

import org.junit.Assert.assertTrue
import org.junit.Test

class GovernanceAITest {
    @Test
    fun `mutate policy increases risk and fanout`() {
        val p = GovernanceAI.RuntimePolicy(gossipFanout = 2, maxPendingMessages = 100, riskLevel = 1)
        val next = GovernanceAI.mutatePolicy(p)
        assertTrue(next.riskLevel >= p.riskLevel)
        assertTrue(next.gossipFanout >= p.gossipFanout)
    }
}
