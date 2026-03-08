package com.bitchat.crypto

/**
 * Represents the Bruno-Iana Capital Stack (BICS) and its four layers.
 */
object BicsService {

    enum class CapitalLayer {
        SURVIVAL,
        STABILITY,
        DISTORTION,
        CONVEXITY
    }

    fun allocateToLayer(layer: CapitalLayer, amount: Double) {
        // placeholder: record allocation
        println("Allocated $$amount to $layer")
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
}
