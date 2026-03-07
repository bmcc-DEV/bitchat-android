package com.ghostpay.android.pay.token

/**
 * Core token data structure — a REBS (Recibo de Entrega de Bem ou Serviço).
 *
 * No personal data is stored. [ownerPubKey] is an ephemeral key-rolled public key
 * that was never linked to any real-world identity on-protocol.
 *
 * [unit] is always a physical unit (kg, L, h, un) — NEVER "BRL" or a fiat currency.
 * This keeps the token outside the legal definition of a "moeda" or "título de crédito".
 */
data class Token(
    /** 32-byte random hex ID, unique per token instance */
    val id: String,
    val type: TokenType,
    /** Quantity in physical units */
    val amount: Double,
    /** Physical unit: "kg", "L", "h", "un", "m²" — never fiat */
    val unit: String,
    /** Human description of the commodity, e.g. "café arábica" */
    val commodity: String,
    /** Epoch millis when this token self-destructs */
    val expiryMs: Long,
    /** Ephemeral owner public key (32 bytes, Schnorr/Ed25519) */
    val ownerPubKey: ByteArray,
    /** Issuer Schnorr signature over canonical bytes (id + amount + unit + expiryMs) */
    val issuerSig: ByteArray,
    /**
     * For FNT_D only: SHA-256 of the (FNT_B.id + FNT_C.id) pair that was burned
     * to create this governance token.
     */
    val burnProof: ByteArray? = null
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiryMs

    /** Canonical byte representation used for signature verification */
    fun canonicalBytes(): ByteArray {
        val idBytes = id.toByteArray(Charsets.UTF_8)
        val amountBytes = java.nio.ByteBuffer.allocate(8).putDouble(amount).array()
        val unitBytes = unit.toByteArray(Charsets.UTF_8)
        val expiryBytes = java.nio.ByteBuffer.allocate(8).putLong(expiryMs).array()
        return idBytes + amountBytes + unitBytes + expiryBytes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Token) return false
        return id == other.id && type == other.type
    }

    override fun hashCode(): Int = id.hashCode() * 31 + type.hashCode()
}
