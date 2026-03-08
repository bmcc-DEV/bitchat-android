package com.bitchat.crypto

/**
 * Tiny exponential-moving-average forecaster used by EdgeOracle.
 */
class EdgeAiForecaster(private val alpha: Double = 0.3) {
    private var ema: Double? = null

    fun ingest(value: Double): Double {
        ema = if (ema == null) value else (alpha * value) + ((1 - alpha) * ema!!)
        return ema!!
    }
}
