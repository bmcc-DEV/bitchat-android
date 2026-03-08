package com.bitchat.crypto.bics

import kotlin.math.exp
import kotlin.math.ln

/**
 * Dynamic Tax Calculator.
 * Calculates optimal tax based on global friction and network efficiency.
 * 
 * Formula:
 *   τ_dyn = min(E[C_swift + I_fiat], ∂U_v/∂T) · e^(-κ·Z_k)
 * 
 * Where:
 *   - C_swift: SWIFT/banking cost + delay penalty
 *   - I_fiat: Real inflation from P2P markets
 *   - ∂U_v/∂T: Network utility gradient
 *   - κ·Z_k: Anonymity bonus (exponential discount)
 */
class DynamicTaxCalculator {
    
    /**
     * Calculate optimal dynamic tax.
     * The tax is always lower than legacy system friction.
     */
    fun calculateOptimalTax(
        swiftCost: Double,           // C_swift: Banking friction (0-10%)
        inflation: Double,            // I_fiat: Real inflation rate (annual %)
        zkStrength: Double,           // Z_k: Anonymity level (0-1)
        networkUtilityGradient: Double = 0.01, // ∂U_v/∂T
        kappa: Double = 2.0          // κ: Anonymity discount factor
    ): Double {
        // Legacy system inefficiency
        val legacyFriction = swiftCost + (inflation / 12.0) // Monthly inflation

        // Network efficiency gradient (lower is better)
        val networkEfficiency = networkUtilityGradient

        // Take minimum (always undercut legacy)
        val baseTax = minOf(legacyFriction, networkEfficiency)

        // Apply anonymity bonus: e^(-κ·Z_k)
        // Higher anonymity → lower tax (incentive for privacy)
        val anonymityDiscount = exp(-kappa * zkStrength)

        return (baseTax * anonymityDiscount).coerceIn(0.001, 0.05) // 0.1% to 5%
    }

    /**
     * Calculate tax for specific transaction.
     */
    fun calculateTransactionTax(
        amount: Double,
        swiftCost: Double,
        inflation: Double,
        zkStrength: Double
    ): Double {
        val rate = calculateOptimalTax(swiftCost, inflation, zkStrength)
        return amount * rate
    }

    /**
     * Calculate deflation power from accumulated taxes.
     * Ω_BRL = lim[t→∞] ∫₀ᵗ (τ_dyn·V_p2p / M_fiat(t)) dt
     * 
     * Simplified: burning rate over time.
     */
    fun calculateDeflationPower(
        taxRate: Double,
        transactionVolume: Double,
        monetaryBase: Double
    ): Double {
        if (monetaryBase <= 0) return 0.0

        // Burning rate: τ·V / M
        val burningRate = (taxRate * transactionVolume) / monetaryBase

        return burningRate.coerceIn(0.0, 0.10) // Cap at 10% deflation
    }

    /**
     * Volatility weaponization: convert panic into profit.
     * P_vol = (σ²_mesh · ln(D_L)) / S_M
     * 
     * Where:
     *   - σ_mesh: Mesh network volatility
     *   - D_L: Liquidity depth
     *   - S_M: Market entropy (panic level)
     */
    fun calculateVolatilityPower(
        volatility: Double,        // σ_mesh: Price volatility (0-1)
        liquidityDepth: Double,    // D_L: Available liquidity
        marketEntropy: Double      // S_M: Market panic (0-∞)
    ): Double {
        if (marketEntropy <= 0 || liquidityDepth <= 0) return 0.0

        // P_vol = (σ² · ln(D_L)) / S_M
        val volatilitySquared = volatility * volatility
        val liquidityFactor = ln(liquidityDepth + 1.0)
        
        val power = (volatilitySquared * liquidityFactor) / marketEntropy

        return power.coerceAtLeast(0.0)
    }

    /**
     * Interest rate for P2P loans (juro fantasma).
     * i_p2p = E[ΔMV/ΔT] + (R₀ + α(D_L/S_L)^κ) + λσ²
     * 
     * Combines Fisher adjustment + liquidity curve + Soros premium.
     */
    fun calculateP2PInterestRate(
        fisherAdjustment: Double,  // E[ΔMV/ΔT]: Inflation protection
        liquidityDemand: Double,   // D_L: Demand for liquidity
        liquiditySupply: Double,   // S_L: Supply of liquidity
        baseRate: Double = 0.03,   // R₀: Base rate (3%)
        alpha: Double = 0.05,      // α: Liquidity sensitivity
        kappa: Double = 2.0,       // κ: Exponential factor
        lambda: Double = 0.5,      // λ: Risk premium factor
        volatility: Double = 0.1   // σ: Asset volatility
    ): Double {
        // Fisher adjustment (inflation protection)
        val fisherComponent = fisherAdjustment

        // Liquidity curve: R₀ + α(D_L/S_L)^κ
        val liquidityRatio = if (liquiditySupply > 0) {
            liquidityDemand / liquiditySupply
        } else {
            10.0 // High cost if no supply
        }
        val liquidityCurve = baseRate + alpha * Math.pow(liquidityRatio, kappa)

        // Soros premium: λσ²
        val sorosPremium = lambda * volatility * volatility

        val totalRate = fisherComponent + liquidityCurve + sorosPremium

        return totalRate.coerceIn(0.01, 0.50) // 1% to 50% APR
    }
}
