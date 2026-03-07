package com.ghostpay.android.pay.ledger

import androidx.room.*

// ── Entities ──────────────────────────────────────────────────────────────────

/**
 * A token held locally by this device (either minted or received).
 * Stored in encrypted Room DB via Tink (already in the project as google-tink-android).
 */
@Entity(tableName = "tokens_mine")
data class TokenEntity(
    @PrimaryKey val id: String,
    val type: String,          // TokenType.name()
    val amount: Double,
    val unit: String,
    val commodity: String,
    val expiryMs: Long,
    val ownerPubKeyHex: String,
    val issuerSigHex: String,
    val burnProofHex: String?,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A tokenized asset in the Dark Bolsa local registry.
 * Gossip-synced with nearby peers (name + unit only, no owner linkage).
 */
@Entity(tableName = "asset_registry")
data class AssetEntity(
    @PrimaryKey val assetId: String,
    val name: String,
    val unit: String,
    val imageHashHex: String?,
    val creatorPubKeyHex: String,
    val currentOwnerPubKeyHex: String,
    val ultimatrixScore: Float,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Open staking position (Renda Fixa Fantasma).
 */
@Entity(tableName = "staking_positions")
data class StakingPositionEntity(
    @PrimaryKey val positionId: String,
    val lockedAmount: Double,
    val unit: String,
    val openedAt: Long,
    val unlockAt: Long,
    val lastYieldCollectedAt: Long,
    val accruedYield: Double
)

/**
 * Local Pay-Bix balance snapshot. Single-row table (id always = 1).
 */
@Entity(tableName = "pay_bix_balances")
data class PayBixEntity(
    @PrimaryKey val id: Int = 1,
    val balance: Double,
    val updatedAt: Long = System.currentTimeMillis()
)

// ── DAOs ──────────────────────────────────────────────────────────────────────

@Dao
interface TokenDao {
    @Query("SELECT * FROM tokens_mine WHERE expiryMs > :nowMs ORDER BY createdAt DESC")
    suspend fun getActiveTokens(nowMs: Long = System.currentTimeMillis()): List<TokenEntity>

    @Query("SELECT * FROM tokens_mine WHERE type = :type AND expiryMs > :nowMs")
    suspend fun getByType(type: String, nowMs: Long = System.currentTimeMillis()): List<TokenEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(token: TokenEntity)

    @Delete
    suspend fun delete(token: TokenEntity)

    @Query("DELETE FROM tokens_mine WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tokens_mine WHERE expiryMs < :nowMs")
    suspend fun purgeExpired(nowMs: Long = System.currentTimeMillis())
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM asset_registry ORDER BY ultimatrixScore DESC")
    suspend fun getAll(): List<AssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(asset: AssetEntity)

    @Query("DELETE FROM asset_registry WHERE assetId = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface StakingDao {
    @Query("SELECT * FROM staking_positions ORDER BY openedAt DESC")
    suspend fun getAll(): List<StakingPositionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(position: StakingPositionEntity)

    @Query("DELETE FROM staking_positions WHERE positionId = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface PayBixDao {
    @Query("SELECT * FROM pay_bix_balances WHERE id = 1")
    suspend fun get(): PayBixEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PayBixEntity)
}
