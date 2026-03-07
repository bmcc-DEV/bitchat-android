package com.ghostpay.android.pay.engine

import com.ghostpay.android.pay.ledger.LedgerRepository
import com.ghostpay.android.pay.ledger.StakingPositionEntity
import java.util.UUID

/**
 * Renda Fixa Fantasma — lock FNT-C commodity tokens to earn i_p2p yield.
 *
 * Your locked amount is used as [liquiditySupplySL] in the market state,
 * making it available for leveraged trades by other mesh participants.
 * Yield is calculated continuously via [InterestRateCalculator].
 */
class StakingManager(
    private val repo: LedgerRepository,
    private val calculator: InterestRateCalculator = InterestRateCalculator
) {

    /**
     * Lock [amount] units ([unit]) for [durationMs] milliseconds.
     * Returns the positionId to reference this position later.
     */
    suspend fun lockBalance(amount: Double, unit: String, durationMs: Long): String {
        val positionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        repo.saveStakingPosition(
            StakingPositionEntity(
                positionId            = positionId,
                lockedAmount          = amount,
                unit                  = unit,
                openedAt              = now,
                unlockAt              = now + durationMs,
                lastYieldCollectedAt  = now,
                accruedYield          = 0.0
            )
        )
        return positionId
    }

    /**
     * Calculate and collect pending yield for a position.
     * Yield = principal * i_p2p * (elapsedSeconds / SECONDS_IN_YEAR)
     */
    suspend fun collectYield(
        positionId: String,
        currentRate: Double
    ): Double {
        val positions = repo.getStakingPositions()
        val position  = positions.firstOrNull { it.positionId == positionId } ?: return 0.0
        val now       = System.currentTimeMillis()
        val elapsedMs = now - position.lastYieldCollectedAt
        val years     = elapsedMs / (365.0 * 24 * 60 * 60 * 1000)
        val yield     = position.lockedAmount * currentRate * years

        repo.saveStakingPosition(
            position.copy(
                lastYieldCollectedAt = now,
                accruedYield         = position.accruedYield + yield
            )
        )
        return yield
    }

    /** Unlock the position and stop yield accrual. Returns total accrued yield. */
    suspend fun unlockBalance(positionId: String, currentRate: Double): Double {
        val collected = collectYield(positionId, currentRate)
        repo.deleteStakingPosition(positionId)
        return collected
    }

    companion object {
        const val SECONDS_IN_YEAR = 365.0 * 24 * 60 * 60
    }
}
