package com.ghostpay.android.pay.engine

/**
 * Snapshot of the local mesh economy at a point in time.
 * Populated from [LedgerSyncManager] gossip state.
 */
data class MarketState(
    /** Transactions per hour recorded by the local mesh window */
    val velocityV: Double,
    /** Cumulative transactions witnessed (proxy for M*V product growth) */
    val totalTxsT: Int,
    /** Estimated local demand for liquidity (open HANDSHAKE count * avg size) */
    val liquidityDemandDL: Double,
    /** Sum of staked amounts currently active in the mesh */
    val liquiditySupplySL: Double,
    /** Rolling 24h variance of the asset being collateralised (Soros premium input) */
    val assetVolatilitySigma2: Double,
    /** Monotonic nonce (not wall-clock time — gossip sequencing) */
    val nonce: Long = 0
)

/**
 * Configuration for the interest rate engine.
 * Defaults tuned for a small local mesh (<100 nodes).
 */
data class EngineConfig(
    /** Base interest rate — minimum floor */
    val r0: Double = 0.02,
    /** Liquidity curve sensitivity coefficient */
    val alpha: Double = 0.05,
    /** Exponential multiplier for the D_L/S_L ratio */
    val kappa: Double = 1.5,
    /** Soros premium risk aversion factor (0 = risk-neutral, 1 = agiota digital) */
    val lambda: Double = 0.3
)
