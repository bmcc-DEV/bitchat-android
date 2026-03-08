package com.bitchat.crypto.storage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "crypto_node_balances",
    indices = [Index(value = ["nodeId", "account"], unique = true)]
)
data class NodeBalanceEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nodeId: String,
    val account: String,
    val balance: Double,
    val timestampMs: Long
)
