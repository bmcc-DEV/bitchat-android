package com.bitchat.crypto.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NetworkLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: NetworkLogEntry)

    @Query("SELECT * FROM crypto_network_logs ORDER BY sequence ASC LIMIT :limit")
    suspend fun latest(limit: Int): List<NetworkLogEntry>
}
