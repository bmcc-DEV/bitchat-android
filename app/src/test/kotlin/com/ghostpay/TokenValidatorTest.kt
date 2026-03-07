package com.ghostpay

import com.ghostpay.android.pay.token.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TokenValidator and ShadowCache.
 * No Android dependencies — runs on the JVM.
 */
class TokenValidatorTest {

    private val signerKey = ByteArray(32) { it.toByte() }
    private lateinit var factory: TokenFactory
    private lateinit var validator: TokenValidator
    private lateinit var cache: ShadowCache

    @Before
    fun setUp() {
        factory   = TokenFactory(signerKey)
        validator = TokenValidator(signerKey)   // same key used as "issuerPublicKey" in stub
        cache     = ShadowCache()
    }

    // ── Single token validity ─────────────────────────────────────────────────

    @Test
    fun valid_fnt_b_passes_validation() {
        val bearer = factory.mintBearer(1.0, "kg", "café")
        assertTrue("Fresh FNT-B must be valid", validator.isValid(bearer))
    }

    @Test
    fun valid_fnt_c_passes_validation() {
        val comm = factory.mintCommodity(1.0, "kg", "café")
        assertTrue("Fresh FNT-C must be valid", validator.isValid(comm))
    }

    @Test
    fun expired_token_fails_validation() {
        val bearer = factory.mintBearer(1.0, "kg", "café")
        // Manually craft an expired copy
        val expired = bearer.copy(expiryMs = System.currentTimeMillis() - 1000)
        assertFalse("Expired token must be invalid", validator.isValid(expired))
    }

    @Test
    fun tampered_signature_fails_validation() {
        val bearer  = factory.mintBearer(1.0, "kg", "café")
        val tampered = bearer.copy(issuerSig = ByteArray(32) { 0xFF.toByte() })
        assertFalse("Tampered signature must be invalid", validator.isValid(tampered))
    }

    @Test
    fun fnt_d_without_burn_proof_fails() {
        val b = factory.mintBearer(1.0, "un", "ticket")
        val c = factory.mintCommodity(1.0, "un", "ticket")
        val d = factory.mintGovernance(b, c).copy(burnProof = null)
        assertFalse("FNT_D without burnProof must be invalid", validator.isValid(d))
    }

    // ── Pair validity ─────────────────────────────────────────────────────────

    @Test
    fun matching_b_c_pair_is_valid() {
        val b = factory.mintBearer(2.0, "L", "cachaça")
        val c = factory.mintCommodity(2.0, "L", "cachaça")
        assertTrue("Matching B+C pair must be valid", validator.isPairValid(b, c))
    }

    @Test
    fun mismatched_amount_pair_is_invalid() {
        val b = factory.mintBearer(2.0, "L", "cachaça")
        val c = factory.mintCommodity(3.0, "L", "cachaça")
        assertFalse("Amount mismatch must be invalid", validator.isPairValid(b, c))
    }

    @Test
    fun mismatched_unit_pair_is_invalid() {
        val b = factory.mintBearer(1.0, "kg", "café")
        val c = factory.mintCommodity(1.0, "L", "café")
        assertFalse("Unit mismatch must be invalid", validator.isPairValid(b, c))
    }

    @Test
    fun wrong_token_types_in_pair_are_invalid() {
        val b = factory.mintBearer(1.0, "kg", "café")
        // Pass two FNT_B tokens — should fail
        assertFalse(validator.isPairValid(b, b))
    }

    // ── ShadowCache (anti-double-spend) ───────────────────────────────────────

    @Test
    fun fresh_pair_is_not_seen() {
        assertFalse("New pair must not be seen", cache.isPairSeen("abc", "def"))
    }

    @Test
    fun marked_pair_is_seen() {
        cache.markPairSeen("aaa", "bbb")
        assertTrue("Marked pair must be seen", cache.isPairSeen("aaa", "bbb"))
    }

    @Test
    fun different_pair_is_not_seen_after_marking_another() {
        cache.markPairSeen("aaa", "bbb")
        assertFalse("Different pair must NOT be seen", cache.isPairSeen("ccc", "ddd"))
    }

    @Test
    fun two_attempts_with_same_pair_detect_double_spend() {
        cache.markPairSeen("x1", "x2")
        assertTrue("Second attempt must be detected", cache.isPairSeen("x1", "x2"))
    }
}
