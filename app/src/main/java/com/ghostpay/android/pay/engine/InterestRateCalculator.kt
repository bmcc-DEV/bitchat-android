package com.ghostpay.android.pay.engine

import kotlin.math.pow

/**
 * Fisher Adjustment — protects against internal mesh inflation.
 *
 * Based on MV = PT:
 * If velocity V rises without a matching increase in real transactions T,
 * the internal price level P is rising (inflation). The expected rate of change
 * of (M*V) per unit of T is the inflation expectation fed into i_p2p.
 *
 * Formula: E[Δ(MV)/ΔT] ≈ (current.V - prev.V) / max(1, ΔT)
 * Clamped to [-0.5, 0.5] to prevent runaway rates.
 */
object FisherAdjustment {
    fun compute(current: MarketState, previous: MarketState): Double {
        val deltaT = (current.totalTxsT - previous.totalTxsT).toDouble().coerceAtLeast(1.0)
        val deltaV = current.velocityV - previous.velocityV
        return (deltaV / deltaT).coerceIn(-0.5, 0.5)
    }
}

/**
 * Liquidity Curve — prices the cost of borrowing based on local supply/demand.
 *
 * Formula: R0 + α * (D_L / S_L)^κ
 *
 * When demand greatly exceeds supply (D_L/S_L >> 1), the curve spikes
 * exponentially (κ > 1), attracting more liquidity providers automatically.
 */
object LiquidityCurve {
    fun compute(state: MarketState, config: EngineConfig): Double {
        val ratio = if (state.liquiditySupplySL <= 0.0) 10.0
                    else (state.liquidityDemandDL / state.liquiditySupplySL).coerceAtLeast(0.0)
        return config.r0 + config.alpha * ratio.pow(config.kappa)
    }
}

/**
 * Soros Premium — punishes exposure to volatile collateral.
 *
 * Formula: λ * σ²
 *
 * High σ² (volatile asset used as guarantee) → high risk premium.
 * Conservative creditor sets λ = 0.1; "agiota digital" sets λ = 1.0.
 */
object SorosPremium {
    fun compute(state: MarketState, config: EngineConfig): Double {
        return (config.lambda * state.assetVolatilitySigma2).coerceAtLeast(0.0)
    }
}

/**
 * The complete i_p2p interest rate calculator.
 *
 * i_p2p = FisherAdjustment + LiquidityCurve + SorosPremium
 *
 * Returns an annual rate as a decimal (e.g. 0.08 = 8% p.a.).
 * Minimum floor: [EngineConfig.r0]. Maximum cap: 5.0 (500% p.a.).
 */
object InterestRateCalculator {

    fun calculate(current: MarketState, previous: MarketState, config: EngineConfig): Double {
        val fisher    = FisherAdjustment.compute(current, previous)
        val liquidity = LiquidityCurve.compute(current, config)
        val soros     = SorosPremium.compute(current, config)
        return (fisher + liquidity + soros).coerceIn(config.r0, 5.0)
    }

    /** Human-readable rate string, e.g. "12.3% a.a." */
    fun formatRate(rate: Double): String = "${"%.1f".format(rate * 100)}% a.a."
}
