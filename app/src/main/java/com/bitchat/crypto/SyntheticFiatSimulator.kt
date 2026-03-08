package com.bitchat.crypto

/**
 * Simulates fiat monetary base contraction via periodic burns.
 */
class SyntheticFiatSimulator(initialSupply: Double) {
    private var supply: Double = initialSupply

    fun burn(amount: Double) {
        if (amount <= 0) return
        supply = (supply - amount).coerceAtLeast(0.0)
        CryptoMetrics.inc("fiat.burn.events")
    }

    fun supply(): Double = supply

    fun impliedPriceLevel(velocity: Double, transactions: Double): Double {
        return DeflationEngine.computePriceLevel(supply, velocity, transactions)
    }
}
