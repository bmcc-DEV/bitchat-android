package com.bitchat.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EncryptionServiceTest {
    @Test
    fun `encrypt-decrypt roundtrip`() {
        val original = "sometext".toByteArray()
        val encrypted = EncryptionService.encrypt(original)
        assertFalse(original.contentEquals(encrypted))
        val decrypted = EncryptionService.decrypt(encrypted)
        assertArrayEquals(original, decrypted)
    }
}
