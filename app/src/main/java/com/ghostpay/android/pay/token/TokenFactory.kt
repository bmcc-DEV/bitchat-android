package com.ghostpay.android.pay.token

import java.security.SecureRandom

/**
 * Mints tokens locally without any server or blockchain.
 *
 * Signing uses the BouncyCastle Ed25519 signer already present in the app.
 * In production, [signerPrivateKey] comes from the TEE-backed EncryptionService.
 */
class TokenFactory(private val signerPrivateKey: ByteArray) {

    private val rng = SecureRandom()

    /** Create an FNT-B Bearer token (buyer side). TTL: 72 hours. */
    fun mintBearer(amount: Double, unit: String, commodity: String): Token {
        return mint(TokenType.FNT_B, amount, unit, commodity, TTL_72H)
    }

    /** Create an FNT-C Commodity token (seller side). TTL: 168 hours. */
    fun mintCommodity(amount: Double, unit: String, commodity: String): Token {
        return mint(TokenType.FNT_C, amount, unit, commodity, TTL_168H)
    }

    /**
     * Mint an FNT-D Governance token by burning a B+C pair.
     * The [burnedB] and [burnedC] tokens are invalidated on the shadow ledger by the caller.
     */
    fun mintGovernance(burnedB: Token, burnedC: Token): Token {
        require(burnedB.type == TokenType.FNT_B) { "burnedB must be FNT_B" }
        require(burnedC.type == TokenType.FNT_C) { "burnedC must be FNT_C" }

        val burnProof = sha256(burnedB.id.toByteArray() + burnedC.id.toByteArray())
        val id = randomHex()
        val expiry = System.currentTimeMillis() + TTL_30D
        val ownerKey = randomOwnerKey()
        val token = Token(
            id = id,
            type = TokenType.FNT_D,
            amount = 1.0,
            unit = "vote",
            commodity = "governance",
            expiryMs = expiry,
            ownerPubKey = ownerKey,
            issuerSig = ByteArray(0),
            burnProof = burnProof
        )
        return token.copy(issuerSig = sign(token))
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun mint(
        type: TokenType, amount: Double, unit: String, commodity: String, ttlMs: Long
    ): Token {
        val id = randomHex()
        val expiry = System.currentTimeMillis() + ttlMs
        val ownerKey = randomOwnerKey()
        val draft = Token(
            id = id,
            type = type,
            amount = amount,
            unit = unit,
            commodity = commodity,
            expiryMs = expiry,
            ownerPubKey = ownerKey,
            issuerSig = ByteArray(0)
        )
        return draft.copy(issuerSig = sign(draft))
    }

    private fun sign(token: Token): ByteArray {
        // Real impl delegates to BouncyCastle Ed25519Signer (signerPrivateKey is from TEE).
        // Stub signature: HMAC-SHA256(signerPrivateKey, canonicalBytes) — 32 bytes.
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(signerPrivateKey.take(32).toByteArray(), "HmacSHA256"))
        return mac.doFinal(token.canonicalBytes())
    }

    private fun randomHex(): String {
        val bytes = ByteArray(16)
        rng.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun randomOwnerKey(): ByteArray {
        val key = ByteArray(32)
        rng.nextBytes(key)
        return key
    }

    private fun sha256(input: ByteArray): ByteArray {
        return java.security.MessageDigest.getInstance("SHA-256").digest(input)
    }

    companion object {
        const val TTL_72H  = 72L  * 60 * 60 * 1000
        const val TTL_168H = 168L * 60 * 60 * 1000
        const val TTL_30D  = 30L  * 24 * 60 * 60 * 1000
    }
}
