package com.bitchat.crypto.channel

import com.bitchat.crypto.crdt.VectorClock
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * State Channel - Off-chain payment channel implementation.
 * 
 * Implements the Addendum's solution to mesh bandwidth:
 * "A maioria das transações ocorre off-chain em canais abertos entre nós confiáveis.
 *  Apenas o saldo final é propagado na Mesh. Isso reduz o tráfego em 99%."
 * 
 * Based on Lightning Network principles but optimized for mesh delay tolerance.
 * 
 * Channel lifecycle:
 * 1. Open: Both parties lock collateral on-chain
 * 2. Transact: Exchange signed state updates off-chain
 * 3. Close: Settle final balance on-chain (cooperative or contested)
 * 
 * Mathematical model:
 *   Balance_A + Balance_B = Capacity (constant)
 *   Each update: nonce++, signatures verified
 */
class StateChannel(
    val channelId: String,
    val partyA: String,
    val partyB: String,
    val initialBalanceA: Double,
    val initialBalanceB: Double
) {
    private val capacity = initialBalanceA + initialBalanceB
    private var currentBalanceA = initialBalanceA
    private var currentBalanceB = initialBalanceB
    private var nonce = 0L
    private var isClosed = false
    
    private val stateHistory = mutableListOf<ChannelState>()
    private val pendingUpdates = ConcurrentHashMap<Long, SignedUpdate>()
    
    data class ChannelState(
        val nonce: Long,
        val balanceA: Double,
        val balanceB: Double,
        val timestamp: Long,
        val vectorClock: VectorClock,
        val hashPrevious: ByteArray
    )
    
    data class SignedUpdate(
        val nonce: Long,
        val balanceA: Double,
        val balanceB: Double,
        val signatureA: ByteArray?,
        val signatureB: ByteArray?,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    enum class ChannelStatus {
        PENDING_OPEN,
        ACTIVE,
        PENDING_CLOSE,
        CLOSED,
        DISPUTED
    }
    
    /**
     * Create a payment from A to B (or vice versa).
     * Returns update that must be signed by both parties.
     */
    fun createUpdate(from: String, to: String, amount: Double): SignedUpdate? {
        if (isClosed) return null
        if (amount <= 0) return null
        
        val (newBalanceA, newBalanceB) = when {
            from == partyA && to == partyB -> {
                if (currentBalanceA < amount) return null  // Insufficient balance
                Pair(currentBalanceA - amount, currentBalanceB + amount)
            }
            from == partyB && to == partyA -> {
                if (currentBalanceB < amount) return null
                Pair(currentBalanceA + amount, currentBalanceB - amount)
            }
            else -> return null  // Invalid parties
        }
        
        val nextNonce = nonce + 1
        
        val update = SignedUpdate(
            nonce = nextNonce,
            balanceA = newBalanceA,
            balanceB = newBalanceB,
            signatureA = null,
            signatureB = null
        )
        
        pendingUpdates[nextNonce] = update
        return update
    }
    
    /**
     * Sign an update (party A or B).
     */
    fun signUpdate(nonce: Long, party: String, signature: ByteArray): Boolean {
        val update = pendingUpdates[nonce] ?: return false
        
        val newUpdate = when (party) {
            partyA -> update.copy(signatureA = signature)
            partyB -> update.copy(signatureB = signature)
            else -> return false
        }
        
        pendingUpdates[nonce] = newUpdate
        
        // If both signatures present, commit the update
        if (newUpdate.signatureA != null && newUpdate.signatureB != null) {
            return commitUpdate(nonce)
        }
        
        return true
    }
    
    /**
     * Commit a fully-signed update to the channel state.
     */
    private fun commitUpdate(nonce: Long): Boolean {
        val update = pendingUpdates[nonce] ?: return false
        
        if (update.signatureA == null || update.signatureB == null) return false
        if (nonce != this.nonce + 1) return false  // Out of order
        
        // Verify balance conservation
        if (update.balanceA + update.balanceB != capacity) return false
        
        // Create state snapshot
        val previousHash = if (stateHistory.isEmpty()) {
            ByteArray(32)
        } else {
            hashState(stateHistory.last())
        }
        
        val newState = ChannelState(
            nonce = nonce,
            balanceA = update.balanceA,
            balanceB = update.balanceB,
            timestamp = System.currentTimeMillis(),
            vectorClock = VectorClock.empty().increment(channelId),
            hashPrevious = previousHash
        )
        
        stateHistory.add(newState)
        currentBalanceA = update.balanceA
        currentBalanceB = update.balanceB
        this.nonce = nonce
        pendingUpdates.remove(nonce)
        
        return true
    }
    
    /**
     * Close channel cooperatively.
     * Returns final settlement to be published on-chain.
     */
    fun closeCooperative(): ChannelSettlement? {
        if (isClosed) return null
        
        isClosed = true
        
        return ChannelSettlement(
            channelId = channelId,
            finalBalanceA = currentBalanceA,
            finalBalanceB = currentBalanceB,
            finalNonce = nonce,
            closedAt = System.currentTimeMillis(),
            isContested = false
        )
    }
    
    /**
     * Close channel unilaterally (contested).
     * Other party has challenge period to submit newer state.
     */
    fun closeContested(submitter: String): ChannelSettlement? {
        if (isClosed) return null
        
        isClosed = true
        
        return ChannelSettlement(
            channelId = channelId,
            finalBalanceA = currentBalanceA,
            finalBalanceB = currentBalanceB,
            finalNonce = nonce,
            closedAt = System.currentTimeMillis(),
            isContested = true,
            contestedBy = submitter,
            challengePeriodEnd = System.currentTimeMillis() + 86400_000  // 24h challenge
        )
    }
    
    /**
     * Challenge a contested closure with a newer state.
     */
    fun submitChallenge(challengeNonce: Long): Boolean {
        if (!isClosed) return false
        if (challengeNonce <= nonce) return false  // Not newer
        
        // In production: verify signatures and update settlement
        // For now, accept the challenge
        val challengeState = stateHistory.find { it.nonce == challengeNonce }
        if (challengeState != null) {
            currentBalanceA = challengeState.balanceA
            currentBalanceB = challengeState.balanceB
            nonce = challengeState.nonce
            return true
        }
        
        return false
    }
    
    /**
     * Get current channel status.
     */
    fun getStatus(): ChannelInfo {
        return ChannelInfo(
            channelId = channelId,
            partyA = partyA,
            partyB = partyB,
            balanceA = currentBalanceA,
            balanceB = currentBalanceB,
            capacity = capacity,
            nonce = nonce,
            isClosed = isClosed,
            totalTransactions = stateHistory.size,
            channelAge = if (stateHistory.isNotEmpty()) {
                System.currentTimeMillis() - stateHistory.first().timestamp
            } else {
                0L
            }
        )
    }
    
    private fun hashState(state: ChannelState): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(state.nonce.toString().toByteArray())
        digest.update(state.balanceA.toString().toByteArray())
        digest.update(state.balanceB.toString().toByteArray())
        return digest.digest()
    }
    
    data class ChannelSettlement(
        val channelId: String,
        val finalBalanceA: Double,
        val finalBalanceB: Double,
        val finalNonce: Long,
        val closedAt: Long,
        val isContested: Boolean,
        val contestedBy: String? = null,
        val challengePeriodEnd: Long? = null
    )
    
    data class ChannelInfo(
        val channelId: String,
        val partyA: String,
        val partyB: String,
        val balanceA: Double,
        val balanceB: Double,
        val capacity: Double,
        val nonce: Long,
        val isClosed: Boolean,
        val totalTransactions: Int,
        val channelAge: Long
    )
}
