package com.ghostpay.android.pay.fx

/**
 * Settlement currency preference stored per-node.
 * Transmitted (encrypted) during PAY_HANDSHAKE so the buyer's app
 * can show the seller what they will receive.
 *
 * IMPORTANT: FNT_INTERNAL means no conversion — settlement stays within the mesh.
 * All external conversions (BRL, BTC, USDT) are *hints* only — the actual
 * off-protocol exchange is done manually by the parties. The protocol never
 * touches fiat or blockchain.
 */
enum class SettlementCurrency(val displayName: String) {
    FNT_INTERNAL("FNT (Rede Fantasma)"),
    BRL_HINT("R\$ (Pix off-protocol)"),
    BTC_HINT("₿ (Bitcoin off-chain)"),
    USDT_HINT("₮ (USDT off-chain)");
}

/**
 * Rate snapshot sourced from gossip — never from a centralized oracle.
 * Rates are expressed as [fntPerUnit], i.e. how many FNT units equal 1 of the target.
 *
 * In the mesh, rates are crowd-sourced: each merchant declares their own FNT price
 * for their goods. The gossip layer averages visible prices into an ambient rate.
 */
data class FxQuote(
    val fntToBrlRate: Double  = 0.0,   // 1 FNT = X BRL (informational only)
    val fntToBtcRate: Double  = 0.0,   // 1 FNT = X BTC satoshis (informational only)
    val fntToUsdtRate: Double = 0.0,   // 1 FNT = X USDT cents (informational only)
    val nonce: Long           = 0L     // gossip nonce — not real timestamp
)

/**
 * Converts FNT amount into a human-readable hint for the seller's preferred currency.
 *
 * This is display-only. No actual conversion happens on-protocol.
 * The result is shown to the buyer as "Seller will receive approximately X".
 */
object FxConverter {

    fun convert(fntAmount: Double, target: SettlementCurrency, quote: FxQuote): String {
        return when (target) {
            SettlementCurrency.FNT_INTERNAL ->
                "%.4f FNT".format(fntAmount)

            SettlementCurrency.BRL_HINT -> {
                require(quote.fntToBrlRate > 0) { "BRL rate not available in gossip" }
                "≈ R\$ ${"%.2f".format(fntAmount * quote.fntToBrlRate)} (off-protocol)"
            }

            SettlementCurrency.BTC_HINT -> {
                require(quote.fntToBtcRate > 0) { "BTC rate not available in gossip" }
                val sats = (fntAmount * quote.fntToBtcRate).toLong()
                "≈ $sats sat (off-chain)"
            }

            SettlementCurrency.USDT_HINT -> {
                require(quote.fntToUsdtRate > 0) { "USDT rate not available in gossip" }
                "≈ ${"%.2f".format(fntAmount * quote.fntToUsdtRate)} USDT (off-chain)"
            }
        }
    }
}

/**
 * Routes settlement: reads the seller's [SettlementCurrency] preference
 * and applies [FxConverter] to generate the display hint for the buyer's UI.
 *
 * Called inside [PaymentFlowManager.processUnlock] just before COMPLETE.
 */
class SettlementRouter {

    private var sellerPreference: SettlementCurrency = SettlementCurrency.FNT_INTERNAL
    private var latestQuote: FxQuote = FxQuote()

    fun setSellerPreference(pref: SettlementCurrency) { sellerPreference = pref }
    fun updateQuote(quote: FxQuote) { latestQuote = quote }

    /**
     * Returns a display hint to show the buyer ("Seller receives ≈ R$ 12.50").
     * Returns null if seller chose FNT_INTERNAL (no conversion hint needed).
     */
    fun settlementHint(fntAmount: Double): String? {
        if (sellerPreference == SettlementCurrency.FNT_INTERNAL) return null
        return try {
            FxConverter.convert(fntAmount, sellerPreference, latestQuote)
        } catch (_: Exception) {
            null
        }
    }
}
