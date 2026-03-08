package com.bitchat.crypto.storage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "crypto_network_logs",
    indices = [Index(value = ["messageId"], unique = true)]
)
data class NetworkLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    val sequence: Long,
    val payload: String,
    val timestampMs: Long
)
