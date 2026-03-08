package com.bitchat.crypto.crdt

import java.util.concurrent.ConcurrentHashMap

/**
 * PN-Counter (Positive-Negative Counter) CRDT Wallet.
 * Implements conflict-free replicated data type for distributed wallets.
 * 
 * Mathematical foundation:
 *   W_x = Σ Inc_i(x) - Σ Dec_i(x)
 * 
 * Properties (Join Semi-Lattice):
 *   - Commutativity: S1 ⊔ S2 = S2 ⊔ S1
 *   - Associativity: (S1 ⊔ S2) ⊔ S3 = S1 ⊔ (S2 ⊔ S3)
 *   - Idempotency: S1 ⊔ S1 = S1
 */
class PNCounterWallet(
    val accountId: String,
    private val increments: ConcurrentHashMap<String, ConcurrentHashMap<VectorClock, Double>> = ConcurrentHashMap(),
    private val decrements: ConcurrentHashMap<String, ConcurrentHashMap<VectorClock, Double>> = ConcurrentHashMap(),
    private val deviceId: String = java.util.UUID.randomUUID().toString()
) {
    private var localClock: VectorClock = VectorClock.empty()
    private val nodeId: String = deviceId // Each device is a unique node

    /**
     * Calculate the balance: W_x = Σ Inc_i(x) - Σ Dec_i(x)
     */
    fun balance(): Double {
        val totalIncrements = increments.values.flatMap { it.values }.sum()
        val totalDecrements = decrements.values.flatMap { it.values }.sum()
        return totalIncrements - totalDecrements
    }

    /**
     * Deposit (increment) funds.
     */
    fun deposit(amount: Double): VectorClock {
        if (amount <= 0) throw IllegalArgumentException("Deposit amount must be positive")
        
        localClock = localClock.increment(nodeId)
        
        increments.computeIfAbsent(nodeId) { ConcurrentHashMap() }[localClock] = amount
        
        return localClock
    }

    /**
     * Withdraw (decrement) funds.
     * Returns false if insufficient balance (anti-entropy protection).
     */
    fun withdraw(amount: Double): VectorClock? {
        if (amount <= 0) throw IllegalArgumentException("Withdraw amount must be positive")
        if (balance() < amount) return null // Veto Ancestral
        
        localClock = localClock.increment(nodeId)
        
        decrements.computeIfAbsent(nodeId) { ConcurrentHashMap() }[localClock] = amount
        
        return localClock
    }

    /**
     * Merge two wallets (⊔ operator).
     * This is the core CRDT operation for offline convergence.
     */
    fun merge(other: PNCounterWallet): PNCounterWallet {
        val mergedIncrements = ConcurrentHashMap<String, ConcurrentHashMap<VectorClock, Double>>()
        val mergedDecrements = ConcurrentHashMap<String, ConcurrentHashMap<VectorClock, Double>>()

        // Merge increments
        val allIncNodes = (increments.keys + other.increments.keys).toSet()
        for (node in allIncNodes) {
            val thisNodeInc = increments[node] ?: ConcurrentHashMap()
            val otherNodeInc = other.increments[node] ?: ConcurrentHashMap()
            
            val mergedNodeInc = ConcurrentHashMap<VectorClock, Double>()
            val allClocks = (thisNodeInc.keys + otherNodeInc.keys).toSet()
            
            for (clock in allClocks) {
                // Take max value at each clock (idempotent)
                val value = maxOf(
                    thisNodeInc[clock] ?: 0.0,
                    otherNodeInc[clock] ?: 0.0
                )
                if (value > 0.0) mergedNodeInc[clock] = value
            }
            
            if (mergedNodeInc.isNotEmpty()) {
                mergedIncrements[node] = mergedNodeInc
            }
        }

        // Merge decrements (same logic)
        val allDecNodes = (decrements.keys + other.decrements.keys).toSet()
        for (node in allDecNodes) {
            val thisNodeDec = decrements[node] ?: ConcurrentHashMap()
            val otherNodeDec = other.decrements[node] ?: ConcurrentHashMap()
            
            val mergedNodeDec = ConcurrentHashMap<VectorClock, Double>()
            val allClocks = (thisNodeDec.keys + otherNodeDec.keys).toSet()
            
            for (clock in allClocks) {
                val value = maxOf(
                    thisNodeDec[clock] ?: 0.0,
                    otherNodeDec[clock] ?: 0.0
                )
                if (value > 0.0) mergedNodeDec[clock] = value
            }
            
            if (mergedNodeDec.isNotEmpty()) {
                mergedDecrements[node] = mergedNodeDec
            }
        }

        // Merge vector clocks
        val mergedWallet = PNCounterWallet(accountId, mergedIncrements, mergedDecrements)
        mergedWallet.localClock = this.localClock.merge(other.localClock)
        
        return mergedWallet
    }

    /**
     * Get transaction history (all increments and decrements).
     */
    fun getHistory(): List<Transaction> {
        val history = mutableListOf<Transaction>()
        
        increments.forEach { (node, ops) ->
            ops.forEach { (clock, amount) ->
                history.add(Transaction(node, clock, amount, TransactionType.DEPOSIT))
            }
        }
        
        decrements.forEach { (node, ops) ->
            ops.forEach { (clock, amount) ->
                history.add(Transaction(node, clock, amount, TransactionType.WITHDRAW))
            }
        }
        
        return history.sortedBy { it.clock.toString() }
    }

    data class Transaction(
        val nodeId: String,
        val clock: VectorClock,
        val amount: Double,
        val type: TransactionType
    )

    enum class TransactionType {
        DEPOSIT, WITHDRAW
    }

    companion object {
        fun create(accountId: String, deviceId: String = java.util.UUID.randomUUID().toString()): PNCounterWallet {
            return PNCounterWallet(accountId, deviceId = deviceId)
        }
    }
}
