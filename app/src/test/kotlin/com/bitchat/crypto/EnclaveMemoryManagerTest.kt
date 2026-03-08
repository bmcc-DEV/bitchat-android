package com.bitchat.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class EnclaveMemoryManagerTest {
    @Test
    fun `can allocate write and read page`() {
        val mgr = EnclaveMemoryManager()
        mgr.allocatePage("p1", 32)
        mgr.write("p1", 0, byteArrayOf(1, 2, 3))
        assertArrayEquals(byteArrayOf(1, 2, 3), mgr.read("p1", 0, 3))
    }
}
