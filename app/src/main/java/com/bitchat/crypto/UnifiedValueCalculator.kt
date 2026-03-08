package com.bitchat.crypto

/**
 * Calculator for the unified value equation:
 * U_v = \u2211_{L=1}^{4} ((C_p2p * Z_k) / \tau_opt) + \u222b \u0394 M_fiat dt
 */
object UnifiedValueCalculator {
    /**
     * Computes the layer contribution for a single layer.
     */
    fun layerContribution(cP2p: Double, zK: Double, tauOpt: Double): Double {
        if (tauOpt == 0.0) throw IllegalArgumentException("tauOpt must be nonzero")
        return (cP2p * zK) / tauOpt
    }

    /**
     * Sums contributions of multiple layers and adds fiat delta integral.
     * The integral is approximated by providing area under \u0394M curve.
     */
    fun computeUnifiedValue(layerContributions: List<Double>, fiatDeltaArea: Double): Double {
        return layerContributions.sum() + fiatDeltaArea
    }
}
