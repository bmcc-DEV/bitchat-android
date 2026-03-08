package com.bitchat.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidKeyStoreKeyManagerTest {
    @Test
    fun `in-memory encryption still roundtrips for phase1 baseline`() {
        // JVM unit-test environment may not provide Android Keystore runtime.
        val plain = "phase1".toByteArray()
        val enc = EncryptionService.encrypt(plain)
        val dec = EncryptionService.decrypt(enc)
        assertArrayEquals(plain, dec)
        assertTrue(enc.isNotEmpty())
    }
}
