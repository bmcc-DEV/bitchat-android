package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class EncryptionServiceHomomorphicTest {
    @Test
    fun `homomorphic add produces correct encrypted sum`() {
        val a = "5".toByteArray()
        val b = "7".toByteArray()
        val encA = EncryptionService.encrypt(a)
        val encB = EncryptionService.encrypt(b)
        val encSum = EncryptionService.homomorphicAdd(encA, encB)
        val sum = String(EncryptionService.decrypt(encSum))
        assertEquals("12.0", sum)
    }
}
