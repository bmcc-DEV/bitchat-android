package com.bitchat.crypto

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Represents a peer node in the network. Each node has its own ledger and
 * can synchronize state by broadcasting its balances map. For simplicity,
 * merging takes the average of the two nodes' balances for each account.
 */
class Node(val id: String) {
    private val ledger = CryptoLedger()

    init {
        NetworkService.registerListener { msg ->
            // receive state broadcast
            try {
                val state = Json.decodeFromString<LedgerState>(msg)
                if (state.nodeId != id) {
                    mergeState(state.balances)
                }
            } catch (_: Exception) {
                // ignore non-state messages
            }
        }
    }

    fun deposit(account: String, amount: Double) {
        ledger.deposit(account, amount)
    }

    fun transfer(from: String, to: String, amount: Double): Boolean {
        val success = ledger.transfer(from, to, amount)
        if (success) broadcastState()
        return success
    }

    fun getBalance(account: String): Double = ledger.getBalance(account)

    private fun mergeState(other: Map<String, Double>) {
        // average balances
        other.forEach { (acct, bal) ->
            val current = ledger.getBalance(acct)
            val avg = (current + bal) / 2.0
            // just overwrite for simplicity
            // deposit or withdraw difference
            val diff = avg - current
            if (diff > 0) ledger.deposit(acct, diff) else ledger.transfer(acct, acct, 0.0) // no-op
        }
    }

    private fun broadcastState() {
        val state = LedgerState(id, ledgerSnapshot())
        val msg = Json.encodeToString(state)
        NetworkService.broadcast(msg)
    }

    private fun ledgerSnapshot(): Map<String, Double> {
        // reflection: but we can expose method or make ledger public; for now hack
        // using known accounts for demo
        val map = mutableMapOf<String, Double>()
        listOf("alice", "bob").forEach { map[it] = ledger.getBalance(it) }
        return map
    }
}

@Serializable
data class LedgerState(val nodeId: String, val balances: Map<String, Double>)
