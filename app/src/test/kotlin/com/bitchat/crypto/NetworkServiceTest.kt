package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkServiceTest {
    @Test
    fun `listeners receive broadcast messages`() {
        var received = ""
        NetworkService.registerListener { msg -> received = msg }
        NetworkService.broadcast("hello")
        assertEquals("hello", received)
    }
}
