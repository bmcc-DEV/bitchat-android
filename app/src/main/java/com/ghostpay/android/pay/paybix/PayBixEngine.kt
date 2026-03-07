package com.ghostpay.android.pay.paybix

import com.ghostpay.android.pay.ledger.LedgerRepository

/**
 * Pay-Bix tokenomics — emission config and reward engine.
 *
 * Emission model (no ICO, no pre-sale):
 *   - 85% distributed via protocol activity (routing, staking, marketplace)
 *   - 10% dev fund (locked, vests over 4 years)
 *   -  5% founder reserve (locked 2 years)
 *
 * Halving: emission rate halves every 210,000 completed transactions
 * (mirrors Bitcoin's halving logic — scarcity grows with adoption).
 */
object PayBixConfig {
    const val TOTAL_SUPPLY: Long            = 21_000_000L
    const val ACTIVITY_ALLOCATION_PERCENT    = 85
    const val DEV_FUND_PERCENT               = 10
    const val FOUNDER_RESERVE_PERCENT        = 5

    // Emission per transaction in base units, before halving
    const val BASE_EMISSION_PER_TX: Double   = 1.0

    /** Halving interval in transactions (same ratio as Bitcoin blocks) */
    const val HALVING_INTERVAL: Long         = 210_000L

    /** Compute the current emission rate given cumulative transaction count. */
    fun emissionRate(totalTxs: Long): Double {
        val halvings = (totalTxs / HALVING_INTERVAL).toInt().coerceAtMost(30)
        return BASE_EMISSION_PER_TX / (1L shl halvings).toDouble()
    }
}

/**
 * Issues Pay-Bix rewards for three categories of protocol activity:
 *
 * - ROUTING: you relayed a mesh payment packet (+small fraction)
 * - STAKING: you provided liquidity (+yield fraction)
 * - MARKETPLACE: you completed a Dark Bolsa trade (+base emission)
 */
class PayBixRewardEngine(private val repo: LedgerRepository) {

    enum class RewardReason { ROUTING, STAKING, MARKETPLACE }

    /**
     * Award Pay-Bix to the local wallet for protocol activity.
     * [totalNetworkTxs] is sourced from [GossipStateVector.totalTxsT].
     */
    suspend fun award(reason: RewardReason, totalNetworkTxs: Long) {
        val base = PayBixConfig.emissionRate(totalNetworkTxs)
        val amount = when (reason) {
            RewardReason.ROUTING     -> base * 0.1   // routing = micro-reward
            RewardReason.STAKING     -> base * 0.5   // staking = medium reward
            RewardReason.MARKETPLACE -> base * 1.0   // full trade = full reward
        }
        repo.addPayBix(amount)
    }
}
