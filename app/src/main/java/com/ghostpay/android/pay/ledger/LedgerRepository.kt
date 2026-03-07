package com.ghostpay.android.pay.ledger

import com.ghostpay.android.pay.token.Token
import com.ghostpay.android.pay.token.TokenType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository layer over the Shadow Ledger Room database.
 * All data stays local — zero network calls.
 */
class LedgerRepository(private val db: ShadowLedgerDatabase) {

    // ── Tokens ─────────────────────────────────────────────────────────────────

    suspend fun saveToken(token: Token) {
        db.tokenDao().insert(token.toEntity())
    }

    suspend fun deleteToken(tokenId: String) {
        db.tokenDao().deleteById(tokenId)
    }

    suspend fun getActiveTokens(): List<Token> {
        return db.tokenDao().getActiveTokens().map { it.toDomain() }
    }

    suspend fun getTokensByType(type: TokenType): List<Token> {
        return db.tokenDao().getByType(type.name).map { it.toDomain() }
    }

    suspend fun purgeExpired() {
        db.tokenDao().purgeExpired()
    }

    // ── Assets ─────────────────────────────────────────────────────────────────

    suspend fun getAssets(): List<AssetEntity> = db.assetDao().getAll()

    suspend fun saveAsset(asset: AssetEntity) = db.assetDao().insertOrUpdate(asset)

    // ── Staking ────────────────────────────────────────────────────────────────

    suspend fun getStakingPositions(): List<StakingPositionEntity> = db.stakingDao().getAll()

    suspend fun saveStakingPosition(position: StakingPositionEntity) {
        db.stakingDao().insertOrUpdate(position)
    }

    suspend fun deleteStakingPosition(positionId: String) {
        db.stakingDao().deleteById(positionId)
    }

    // ── Pay-Bix ────────────────────────────────────────────────────────────────

    suspend fun getPayBixBalance(): Double = db.payBixDao().get()?.balance ?: 0.0

    suspend fun setPayBixBalance(amount: Double) {
        db.payBixDao().upsert(PayBixEntity(balance = amount))
    }

    suspend fun addPayBix(delta: Double) {
        val current = getPayBixBalance()
        setPayBixBalance(current + delta)
    }

    // ── Mappers ────────────────────────────────────────────────────────────────

    private fun Token.toEntity() = TokenEntity(
        id = id,
        type = type.name,
        amount = amount,
        unit = unit,
        commodity = commodity,
        expiryMs = expiryMs,
        ownerPubKeyHex = ownerPubKey.toHex(),
        issuerSigHex = issuerSig.toHex(),
        burnProofHex = burnProof?.toHex()
    )

    private fun TokenEntity.toDomain(): Token = Token(
        id = id,
        type = TokenType.valueOf(type),
        amount = amount,
        unit = unit,
        commodity = commodity,
        expiryMs = expiryMs,
        ownerPubKey = ownerPubKeyHex.hexToBytes(),
        issuerSig = issuerSigHex.hexToBytes(),
        burnProof = burnProofHex?.hexToBytes()
    )

    // ── Byte/Hex helpers ───────────────────────────────────────────────────────

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        val result = ByteArray(length / 2)
        for (i in result.indices) result[i] = substring(i * 2, i * 2 + 2).toInt(16).toByte()
        return result
    }
}
