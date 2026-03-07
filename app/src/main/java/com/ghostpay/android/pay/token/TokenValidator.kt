package com.ghostpay.android.pay.token

/**
 * Validates tokens before acceptance.
 *
 * Rules (in order):
 * 1. Not expired
 * 2. Issuer signature is authentic
 * 3. For FNT_D: burn proof field is present
 * 4. For a B+C pair: amount parity check (same physical amount)
 */
class TokenValidator(private val issuerPublicKey: ByteArray) {

    /** Validate a single token. */
    fun isValid(token: Token): Boolean {
        if (token.isExpired) return false
        if (!verifySignature(token)) return false
        if (token.type == TokenType.FNT_D && token.burnProof == null) return false
        return true
    }

    /**
     * Validate a B+C pair for a complete atomic swap.
     * Both tokens must be valid, and their physical amounts must match.
     */
    fun isPairValid(bearer: Token, commodity: Token): Boolean {
        if (bearer.type != TokenType.FNT_B) return false
        if (commodity.type != TokenType.FNT_C) return false
        if (!isValid(bearer)) return false
        if (!isValid(commodity)) return false
        // Physical quantity must match (within floating-point tolerance)
        if (Math.abs(bearer.amount - commodity.amount) > 0.001) return false
        // Unit must match
        if (bearer.unit != commodity.unit) return false
        return true
    }

    // ── Private ────────────────────────────────────────────────────────────────

    private fun verifySignature(token: Token): Boolean {
        return try {
            // Production: BouncyCastle Ed25519Signer.verifySignature(issuerPublicKey, canonical, sig)
            // Stub: HMAC-SHA256 verify (same key used in TokenFactory.sign)
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            mac.init(javax.crypto.spec.SecretKeySpec(issuerPublicKey.take(32).toByteArray(), "HmacSHA256"))
            val expected = mac.doFinal(token.canonicalBytes())
            expected.contentEquals(token.issuerSig)
        } catch (_: Exception) {
            false
        }
    }
}
