package com.bitchat.crypto

import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeOracleTest {
    @Test
    fun `oracle returns formatted price for symbol`() {
        val result = EdgeOracle.fetchRealWorldData("btc")
        assertTrue(result.startsWith("BTC:"))
    }
}
