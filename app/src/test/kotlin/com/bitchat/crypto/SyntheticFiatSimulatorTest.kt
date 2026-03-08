package com.bitchat.crypto

import org.junit.Assert.assertTrue
import org.junit.Test

class SyntheticFiatSimulatorTest {
    @Test
    fun `burn reduces supply`() {
        val sim = SyntheticFiatSimulator(1000.0)
        sim.burn(100.0)
        assertTrue(sim.supply() < 1000.0)
    }
}
