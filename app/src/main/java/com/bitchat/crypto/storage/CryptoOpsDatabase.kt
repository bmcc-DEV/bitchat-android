package com.bitchat.crypto.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [NetworkLogEntry::class, NodeBalanceEntry::class], version = 3, exportSchema = false)
abstract class CryptoOpsDatabase : RoomDatabase() {
    abstract fun networkLogDao(): NetworkLogDao

    companion object {
        @Volatile private var instance: CryptoOpsDatabase? = null

        fun get(context: Context): CryptoOpsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CryptoOpsDatabase::class.java,
                    "crypto_ops.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
