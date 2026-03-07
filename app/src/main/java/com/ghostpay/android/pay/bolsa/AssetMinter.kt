package com.ghostpay.android.pay.bolsa

import com.ghostpay.android.pay.ledger.AssetEntity
import com.ghostpay.android.pay.ledger.LedgerRepository
import com.ghostpay.android.pay.token.Token
import com.ghostpay.android.pay.token.TokenFactory
import java.util.UUID

/**
 * Converts a physical asset into an on-shelf FNT-C token.
 *
 * Flow:
 * 1. User provides name, unit, quantity and optional image hash
 * 2. [AssetMinter] creates an [AssetEntity] in the local registry
 * 3. Also mints an FNT-C with commodity = asset name for trading
 * 4. NFC transfer: approximate token swap via [PaymentFlowManager]
 */
class AssetMinter(
    private val repo: LedgerRepository,
    private val factory: TokenFactory,
    private val myPubKeyHex: String
) {

    /**
     * Tokenize an asset locally.
     * [imageHash] is a SHA-256 hex of the photo bytes (stored locally only).
     * Returns the newly minted FNT-C token.
     */
    suspend fun mintAsset(
        name: String,
        quantity: Double,
        unit: String,
        imageHash: String? = null,
        isDisruptive: Boolean = false
    ): Pair<AssetEntity, Token> {
        val assetId = UUID.randomUUID().toString()
        val score   = UltimatrixFilter.score(quantity, isDisruptive)

        val asset = AssetEntity(
            assetId              = assetId,
            name                 = name,
            unit                 = unit,
            imageHashHex         = imageHash,
            creatorPubKeyHex     = myPubKeyHex,
            currentOwnerPubKeyHex = myPubKeyHex,
            ultimatrixScore      = score
        )
        repo.saveAsset(asset)

        val token = factory.mintCommodity(quantity, unit, name)
        repo.saveToken(token)

        return asset to token
    }

    /**
     * Transfer asset ownership after a successful NFC swap.
     * [newOwnerPubKeyHex] is the buyer's ephemeral public key from the HANDSHAKE.
     */
    suspend fun transferOwnership(assetId: String, newOwnerPubKeyHex: String) {
        val assets  = repo.getAssets()
        val current = assets.firstOrNull { it.assetId == assetId } ?: return
        repo.saveAsset(current.copy(currentOwnerPubKeyHex = newOwnerPubKeyHex))
    }
}

/**
 * Scores an asset on the Ultimatrix 3-axis filter.
 *
 * Axis A — Rockefeller (Efficiency): low-cost production signals high survival.
 * Axis B — Soros       (Asymmetry):  high local demand vs supply → market edge.
 * Axis C — Musk        (Disruption): creator declares whether asset is truly novel.
 *
 * Assets with score < 0.2 are hidden from the Dark Bolsa feed.
 */
object UltimatrixFilter {

    /**
     * Quick score based on available metadata at mint time.
     * In a multi-peer session, axes B and C are updated by the gossip layer.
     */
    fun score(
        quantity: Double,
        isDisruptive: Boolean,
        localDemandRatio: Double = 1.0  // D_L / S_L for this asset type
    ): Float {
        val axisA = (1.0 / maxOf(1.0, quantity / 100.0)).coerceIn(0.0, 1.0)  // normalised to 100 units
        val axisB = localDemandRatio.coerceIn(0.0, 1.0)
        val axisC = if (isDisruptive) 1.0 else 0.3

        return ((axisA * 0.3 + axisB * 0.4 + axisC * 0.3)).coerceIn(0.0, 1.0).toFloat()
    }

    fun isPassing(score: Float): Boolean = score >= 0.2f
}
