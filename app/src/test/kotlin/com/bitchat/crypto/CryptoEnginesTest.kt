package com.bitchat.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

class CryptoEnginesTest {

    @Test
    fun `deflation price level computes correctly`() {
        val p = DeflationEngine.computePriceLevel(100.0, 2.0, 50.0)
        assertEquals(4.0, p, 1e-6)
    }

    @Test
    fun `unified value calculates layers plus fiat delta`() {
        val layers = listOf(1.0, 2.0, 3.0, 4.0)
        val uv = UnifiedValueCalculator.computeUnifiedValue(layers, 5.0)
        assertEquals(15.0, uv, 1e-6)
    }

    @Test
    fun `thermodynamics kinetic energy formula`() {
        val ke = ThermodynamicsEngine.kineticEnergy(2.0, 3.0)
        assertEquals(9.0, ke, 1e-6)
    }

    @Test
    fun `bics layer return calculations`() {
        val ret = BicsService.computeLayerReturn(BicsService.CapitalLayer.SURVIVAL, 100.0)
        assertEquals(98.0, ret, 1e-6)
    }
}
