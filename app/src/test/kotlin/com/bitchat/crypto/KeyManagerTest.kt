package com.bitchat.crypto

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class KeyManagerTest {
    @Test
    fun `returns same key for same alias`() {
        val a = KeyManager.getOrCreate("alias1")
        val b = KeyManager.getOrCreate("alias1")
        assertNotNull(a)
        assertSame(a, b)
    }
}
