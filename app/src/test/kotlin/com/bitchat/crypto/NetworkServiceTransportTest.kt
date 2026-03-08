package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkServiceTransportTest {
    @Test
    fun `ordered listener still works without transport`() {
        NetworkService.resetForTests()
        var seen = 0
        NetworkService.registerOrderedListener { seen++ }
        NetworkService.broadcast("hello")
        assertEquals(1, seen)
    }
}
