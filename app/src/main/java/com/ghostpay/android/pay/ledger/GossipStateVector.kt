package com.ghostpay.android.pay.ledger

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Anonymous state vector shared via the gossip protocol.
 *
 * Only aggregate scalars are exchanged — no raw token data, no identities.
 * This satisfies LGPD art. 5 since nothing in this vector qualifies as
 * "dado pessoal identificável".
 *
 * Fields:
 *  - [velocityV]       Transactions per hour in the local mesh over the last window
 *  - [totalTxsT]       Cumulative transaction count this node has witnessed
 *  - [liquiditySL]     Sum of staked amounts currently in the local mesh (anonymous)
 *  - [liquidityDL]     Estimated demand: count of open HANDSHAKE packets * avg request size
 *  - [nodeCount]       How many distinct anonymous nodes contributed to this vector
 *  - [updatedAt]       Nonce-sequenced (NOT wall-clock time — just a monotonic counter)
 */
data class GossipStateVector(
    val velocityV: Double = 0.0,
    val totalTxsT: Int    = 0,
    val liquiditySL: Double = 0.0,
    val liquidityDL: Double = 0.0,
    val nodeCount: Int    = 1,
    val updatedAt: Long   = 0L         // monotonic nonce, not real timestamp
) {
    /** Merge two vectors by weighted average (RSSI-weighted in real impl, equal here). */
    fun merge(other: GossipStateVector): GossipStateVector = GossipStateVector(
        velocityV   = (velocityV + other.velocityV) / 2.0,
        totalTxsT   = totalTxsT + other.totalTxsT,
        liquiditySL = liquiditySL + other.liquiditySL,
        liquidityDL = liquidityDL + other.liquidityDL,
        nodeCount   = nodeCount + other.nodeCount,
        updatedAt   = maxOf(updatedAt, other.updatedAt)
    )

    /** Compact 40-byte binary serialisation for BLE payload. */
    fun encode(): ByteArray {
        val buf = java.nio.ByteBuffer.allocate(40)
        buf.putDouble(velocityV)
        buf.putInt(totalTxsT)
        buf.putDouble(liquiditySL)
        buf.putDouble(liquidityDL)
        buf.putInt(nodeCount)
        buf.putLong(updatedAt)
        return buf.array()
    }

    companion object {
        fun decode(bytes: ByteArray): GossipStateVector? {
            if (bytes.size < 40) return null
            val buf = java.nio.ByteBuffer.wrap(bytes)
            return GossipStateVector(
                velocityV   = buf.double,
                totalTxsT   = buf.int,
                liquiditySL = buf.double,
                liquidityDL = buf.double,
                nodeCount   = buf.int,
                updatedAt   = buf.long
            )
        }
    }
}

/**
 * Manages the local gossip state vector and merges incoming updates from peers.
 */
class LedgerSyncManager {

    private val mutex = Mutex()
    private var _state = GossipStateVector()

    val state: GossipStateVector get() = _state

    /** Record a completed local transaction (bumps velocity and total count). */
    suspend fun recordLocalTransaction(stakedAmount: Double = 0.0) = mutex.withLock {
        _state = _state.copy(
            velocityV   = _state.velocityV + 1.0,
            totalTxsT   = _state.totalTxsT + 1,
            liquiditySL = _state.liquiditySL + stakedAmount,
            updatedAt   = _state.updatedAt + 1
        )
    }

    /** Accept an incoming gossip vector from a peer (via BLE). */
    suspend fun mergeIncoming(incoming: GossipStateVector) = mutex.withLock {
        if (incoming.updatedAt > _state.updatedAt) {
            _state = _state.merge(incoming)
        }
    }

    /** Serialise local state for gossip broadcast. */
    fun encodeForBroadcast(): ByteArray = _state.encode()
}
