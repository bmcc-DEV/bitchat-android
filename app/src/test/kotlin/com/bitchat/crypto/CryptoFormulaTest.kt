package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class CryptoFormulaTest {
    @Test
    fun `layer contribution formula works`() {
        val contrib = UnifiedValueCalculator.layerContribution(2.0, 5.0, 1.0)
        assertEquals(10.0, contrib, 1e-6)
    }

    @Test
    fun `shielding integral approximation`() {
        // integral of f(x)=x from 0 to 1 should be 0.5
        val result = ThermodynamicsEngine.shieldingIntegral({ it }, 0.0, 1.0, steps = 10000)
        assertEquals(0.5, result, 1e-3)
    }
}
