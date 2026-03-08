package com.bitchat.crypto.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object NodeStateRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var dao: NetworkLogDao? = null

    fun init(dao: NetworkLogDao) {
        this.dao = dao
    }

    fun saveSnapshotAsync(nodeId: String, balances: Map<String, Double>) {
        val d = dao ?: return
        scope.launch {
            d.deleteNodeBalances(nodeId)
            val rows = balances.map {
                NodeBalanceEntry(
                    nodeId = nodeId,
                    account = it.key,
                    balance = it.value,
                    timestampMs = System.currentTimeMillis()
                )
            }
            if (rows.isNotEmpty()) d.upsertNodeBalances(rows)
        }
    }

    suspend fun loadSnapshot(nodeId: String): Map<String, Double> {
        val d = dao ?: return emptyMap()
        return d.balancesForNode(nodeId).associate { it.account to it.balance }
    }
}
