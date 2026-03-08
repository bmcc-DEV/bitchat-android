package com.bitchat.crypto

/**
 * Represents the synthetic deflation equation M * V = P * T.
 * This engine provides methods to compute missing variables given the rest.
 */
object DeflationEngine {
    /**
     * Computes price level P given money supply M, velocity V and transactions T.
     */
    fun computePriceLevel(moneySupply: Double, velocity: Double, transactions: Double): Double {
        if (transactions == 0.0) throw IllegalArgumentException("transactions must be nonzero")
        return (moneySupply * velocity) / transactions
    }

    // Additional helper methods can be added as needed.
}
