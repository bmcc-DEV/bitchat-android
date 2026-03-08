package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkServiceOrderedTest {
    @Test
    fun `deduplicates ordered messages by id`() {
        NetworkService.resetForTests()
        var count = 0
        NetworkService.registerOrderedListener { count++ }
        val m = NetworkService.NetworkMessage("id-1", 1, "x")
        NetworkService.broadcastOrdered(m)
        NetworkService.broadcastOrdered(m)
        assertEquals(1, count)
    }
}
