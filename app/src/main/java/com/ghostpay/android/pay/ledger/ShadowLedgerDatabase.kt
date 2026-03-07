package com.ghostpay.android.pay.ledger

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Shadow Ledger Room Database.
 *
 * "Shadow" because it is fully local, encrypted by Tink (AES-256-GCM via
 * EncryptedSharedPreferences key), and invisible to any external observer.
 *
 * No network sync — gossip protocol only shares anonymous state vectors,
 * never raw token data.
 */
@Database(
    entities = [
        TokenEntity::class,
        AssetEntity::class,
        StakingPositionEntity::class,
        PayBixEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShadowLedgerDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao
    abstract fun assetDao(): AssetDao
    abstract fun stakingDao(): StakingDao
    abstract fun payBixDao(): PayBixDao

    companion object {
        @Volatile
        private var INSTANCE: ShadowLedgerDatabase? = null

        fun getInstance(context: Context): ShadowLedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShadowLedgerDatabase::class.java,
                    "shadow_ledger.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
