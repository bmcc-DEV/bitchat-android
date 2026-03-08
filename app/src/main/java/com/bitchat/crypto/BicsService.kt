package com.bitchat.crypto

/**
 * Represents the Bruno-Iana Capital Stack (BICS) and its four layers.
 */
object BicsService {
    private val allocations = mutableMapOf<CapitalLayer, Double>()

    enum class CapitalLayer {
        SURVIVAL,
        STABILITY,
        DISTORTION,
        CONVEXITY
    }

    fun allocateToLayer(layer: CapitalLayer, amount: Double) {
        allocations[layer] = allocations.getOrDefault(layer, 0.0) + amount
        println("Allocated $$amount to $layer")
        CryptoMetrics.inc("bics.allocations")
    }

    fun computeLayerReturn(layer: CapitalLayer, amount: Double): Double {
        // stubbed return calculation
        return when (layer) {
            CapitalLayer.SURVIVAL -> amount * 0.98
            CapitalLayer.STABILITY -> amount * 1.02
            CapitalLayer.DISTORTION -> amount * 1.1
            CapitalLayer.CONVEXITY -> amount * 2.0
        }
    }

    fun getAllocation(layer: CapitalLayer): Double = allocations.getOrDefault(layer, 0.0)

    /**
     * Simple synthetic-fiat deflation simulator based on M*V=P*T.
     */
    fun simulateDeflation(moneySupply: Double, velocity: Double, transactions: Double): Double {
        return DeflationEngine.computePriceLevel(moneySupply, velocity, transactions)
    }
}
