package com.bitchat.crypto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZkProofServiceTest {
    @Test
    fun `proof verifies correct statement`() {
        val stmt = "alice->bob:50"
        val proof = ZkProofService.prove(stmt)
        assertTrue(ZkProofService.verify(stmt, proof))
    }

    @Test
    fun `proof fails wrong statement`() {
        val stmt = "alice->bob:50"
        val proof = ZkProofService.prove(stmt)
        assertFalse(ZkProofService.verify("alice->bob:60", proof))
    }
}
