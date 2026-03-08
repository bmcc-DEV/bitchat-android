package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class ConsensusEngineTest {
    @Test
    fun `orders by sequence then id`() {
        val input = listOf(
            NetworkService.NetworkMessage("b", 2, "x"),
            NetworkService.NetworkMessage("a", 1, "x"),
            NetworkService.NetworkMessage("c", 2, "x")
        )
        val ordered = ConsensusEngine.order(input)
        assertEquals(listOf("a", "b", "c"), ordered.map { it.id })
    }
}
