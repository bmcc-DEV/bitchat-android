package com.bitchat.crypto.velocity

import kotlin.math.ln
import kotlin.math.pow

/**
 * Velocity Monitor - Tracks and weaponizes money velocity.
 * 
 * Implements the Addendum's economic attack:
 * "O objetivo não é deletar os dígitos do Banco Central (impossível), 
 *  é tornar o BRL **inútil para transação interna**, colapsando sua 
 *  velocidade (V)."
 * 
 * Based on Fisher's equation: M × V = P × Q
 * Where:
 * - M = Money supply (controlled by central bank)
 * - V = Velocity of money (controlled by us)
 * - P = Price level
 * - Q = Real output
 * 
 * Strategy:
 * 1. Monitor V_fiat (fiat velocity in the real economy)
 * 2. Maximize V_mesh (mesh velocity via state channels)
 * 3. When V_fiat drops below threshold → Mesh has won
 */
class VelocityMonitor {
    
    private val fiatTransactions = mutableListOf<Transaction>()
    private val meshTransactions = mutableListOf<Transaction>()
    private val velocitySamples = mutableListOf<VelocitySample>()
    
    data class Transaction(
        val id: String,
        val amount: Double,
        val timestamp: Long,
        val currency: Currency
    )
    
    enum class Currency {
        FIAT,  // BRL, USD, etc.
        MESH   // BICS tokens
    }
    
    data class VelocitySample(
        val timestamp: Long,
        val V_fiat: Double,      // Fiat velocity
        val V_mesh: Double,      // Mesh velocity
        val M_fiat: Double,      // Fiat money supply
        val M_mesh: Double,      // Mesh money supply
        val ratio: Double        // V_mesh / V_fiat
    )
    
    /**
     * Record a transaction.
     */
    fun recordTransaction(tx: Transaction) {
        when (tx.currency) {
            Currency.FIAT -> fiatTransactions.add(tx)
            Currency.MESH -> meshTransactions.add(tx)
        }
    }
    
    /**
     * Calculate velocity of money.
     * Formula: V = (Σ transactions in period) / (average money supply)
     * 
     * Simplified: V = Total Transaction Volume / Money Supply
     */
    fun calculateVelocity(
        currency: Currency,
        windowMs: Long = 86400_000  // 24 hours
    ): Double {
        val now = System.currentTimeMillis()
        val transactions = when (currency) {
            Currency.FIAT -> fiatTransactions.filter { now - it.timestamp < windowMs }
            Currency.MESH -> meshTransactions.filter { now - it.timestamp < windowMs }
        }
        
        if (transactions.isEmpty()) return 0.0
        
        val totalVolume = transactions.sumOf { it.amount }
        val moneySupply = estimateMoneySupply(currency)
        
        if (moneySupply == 0.0) return 0.0
        
        // Annualize: V = (Volume / Supply) × (365 days / window days)
        val windowDays = windowMs / 86400_000.0
        val velocity = (totalVolume / moneySupply) * (365.0 / windowDays)
        
        return velocity
    }
    
    /**
     * Estimate money supply (M).
     * For mesh: sum of all wallet balances.
     * For fiat: proxy using transaction averages.
     */
    private fun estimateMoneySupply(currency: Currency): Double {
        return when (currency) {
            Currency.FIAT -> {
                // Proxy: average transaction amount × unique participants
                val recentTx = fiatTransactions.takeLast(100)
                if (recentTx.isEmpty()) 1000.0 else {
                    recentTx.map { it.amount }.average() * 100  // Rough estimate
                }
            }
            Currency.MESH -> {
                // In production: query CryptoLedger for total supply
                10000.0  // Placeholder
            }
        }
    }
    
    /**
     * Take a snapshot of current velocity state.
     */
    fun takeSnapshot(): VelocitySample {
        val V_fiat = calculateVelocity(Currency.FIAT)
        val V_mesh = calculateVelocity(Currency.MESH)
        val M_fiat = estimateMoneySupply(Currency.FIAT)
        val M_mesh = estimateMoneySupply(Currency.MESH)
        
        val ratio = if (V_fiat > 0) V_mesh / V_fiat else Double.MAX_VALUE
        
        val sample = VelocitySample(
            timestamp = System.currentTimeMillis(),
            V_fiat = V_fiat,
            V_mesh = V_mesh,
            M_fiat = M_fiat,
            M_mesh = M_mesh,
            ratio = ratio
        )
        
        velocitySamples.add(sample)
        
        // Keep last 365 samples (1 year if daily snapshots)
        if (velocitySamples.size > 365) {
            velocitySamples.removeAt(0)
        }
        
        return sample
    }
    
    /**
     * Check if mesh has achieved velocity dominance.
     * Criteria: V_mesh > 2 × V_fiat for sustained period.
     */
    fun hasMeshWon(): Boolean {
        if (velocitySamples.size < 30) return false  // Need 30 samples minimum
        
        val recentSamples = velocitySamples.takeLast(30)
        val dominantSamples = recentSamples.count { it.ratio > 2.0 }
        
        // 80% of recent samples show dominance
        return dominantSamples >= (recentSamples.size * 0.8)
    }
    
    /**
     * Calculate velocity collapse index.
     * Measures rate of V_fiat decline.
     * 
     * Formula: VCI = -Δ(log V_fiat) / Δt
     * Higher VCI = faster collapse
     */
    fun calculateCollapseIndex(): Double {
        if (velocitySamples.size < 2) return 0.0
        
        val recent = velocitySamples.takeLast(10)
        val first = recent.first()
        val last = recent.last()
        
        if (first.V_fiat <= 0 || last.V_fiat <= 0) return 0.0
        
        val deltaLogV = ln(last.V_fiat) - ln(first.V_fiat)
        val deltaT = (last.timestamp - first.timestamp) / 86400_000.0  // Days
        
        if (deltaT == 0.0) return 0.0
        
        return -deltaLogV / deltaT  // Negative because we want positive = collapse
    }
    
    /**
     * Predict fiat velocity in N days using exponential decay model.
     * V(t) = V_0 × e^(-λt)
     * Where λ = collapse rate
     */
    fun predictFiatVelocity(daysAhead: Int): Double {
        if (velocitySamples.isEmpty()) return 0.0
        
        val V_current = velocitySamples.last().V_fiat
        val collapseRate = calculateCollapseIndex()
        
        return V_current * Math.exp(-collapseRate * daysAhead)
    }
    
    /**
     * Calculate economic impact.
     * Returns estimated % of economy using mesh vs fiat.
     */
    fun calculateEconomicShare(): EconomicShare {
        val latest = velocitySamples.lastOrNull() ?: return EconomicShare(0.0, 0.0, 0.0)
        
        // Share proportional to V × M (total economic activity)
        val fiatActivity = latest.V_fiat * latest.M_fiat
        val meshActivity = latest.V_mesh * latest.M_mesh
        val totalActivity = fiatActivity + meshActivity
        
        if (totalActivity == 0.0) return EconomicShare(0.0, 0.0, 0.0)
        
        val meshShare = meshActivity / totalActivity
        val fiatShare = fiatActivity / totalActivity
        
        // Calculate displacement rate (% per month)
        val displacementRate = if (velocitySamples.size >= 30) {
            val monthAgo = velocitySamples[velocitySamples.size - 30]
            val oldShare = (monthAgo.V_mesh * monthAgo.M_mesh) / 
                          ((monthAgo.V_fiat * monthAgo.M_fiat) + (monthAgo.V_mesh * monthAgo.M_mesh))
            (meshShare - oldShare) * 100  // % change
        } else {
            0.0
        }
        
        return EconomicShare(
            meshShare = meshShare,
            fiatShare = fiatShare,
            displacementRatePerMonth = displacementRate
        )
    }
    
    /**
     * Get strategic dashboard data.
     */
    fun getStrategicDashboard(): StrategicDashboard {
        val latest = takeSnapshot()
        val collapseIndex = calculateCollapseIndex()
        val economicShare = calculateEconomicShare()
        val meshVictory = hasMeshWon()
        val daysToVictory = if (meshVictory) 0 else estimateDaysToVictory()
        
        return StrategicDashboard(
            currentVelocityRatio = latest.ratio,
            fiatVelocity = latest.V_fiat,
            meshVelocity = latest.V_mesh,
            collapseIndex = collapseIndex,
            economicShare = economicShare,
            meshVictory = meshVictory,
            estimatedDaysToVictory = daysToVictory,
            historicalSamples = velocitySamples.size
        )
    }
    
    /**
     * Estimate days until mesh dominance.
     */
    private fun estimateDaysToVictory(): Int {
        if (velocitySamples.size < 10) return -1  // Not enough data
        
        val collapseRate = calculateCollapseIndex()
        if (collapseRate <= 0) return -1  // Not collapsing
        
        val latest = velocitySamples.last()
        val targetRatio = 2.0  // V_mesh / V_fiat = 2
        
        // Solve: V_mesh / (V_fiat × e^(-λt)) = 2
        // t = ln(V_fiat / (V_mesh / 2)) / λ
        val currentRatio = latest.ratio
        if (currentRatio >= targetRatio) return 0
        
        val t = ln(targetRatio / currentRatio) / collapseRate
        return t.toInt().coerceAtLeast(0)
    }
    
    data class EconomicShare(
        val meshShare: Double,           // 0-1
        val fiatShare: Double,           // 0-1
        val displacementRatePerMonth: Double  // % per month
    )
    
    data class StrategicDashboard(
        val currentVelocityRatio: Double,     // V_mesh / V_fiat
        val fiatVelocity: Double,
        val meshVelocity: Double,
        val collapseIndex: Double,            // Rate of fiat velocity collapse
        val economicShare: EconomicShare,
        val meshVictory: Boolean,             // Has mesh achieved dominance?
        val estimatedDaysToVictory: Int,      // -1 if indeterminate
        val historicalSamples: Int
    )
}
