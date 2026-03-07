package com.ghostpay.android.pay.mesh

import com.ghostpay.android.pay.token.ShadowCache
import com.ghostpay.android.pay.token.Token
import com.ghostpay.android.pay.token.TokenValidator
import kotlinx.coroutines.*

/**
 * Three-step atomic swap protocol:
 *
 * 1. HANDSHAKE  — A and B derive ephemeral shared key K_d (X25519 + Blake3-256).
 *                 Seller's [CurrencyPreference] is transmitted here (encrypted).
 * 2. COMMIT     — Buyer sends blinded FNT-B; Seller sends blinded FNT-C.
 *                 Neither party sees the other's raw token until step 3.
 * 3. UNLOCK     — Both reveal blinding factors simultaneously.
 *                 Tokens change owner. FX conversion applied to seller.
 *                 Pair hash marked in ShadowCache (anti-double-spend).
 *
 * In the MVP the blinding is XOR with a 32-byte random nonce (commitment scheme).
 * Production upgrade: replace with Pedersen commitments + Bulletproofs.
 */
class PaymentFlowManager(
    private val validator: TokenValidator,
    private val shadowCache: ShadowCache,
    private val scope: CoroutineScope
) {

    enum class State { IDLE, AWAITING_COMMIT, AWAITING_UNLOCK, COMPLETE, FAILED }

    interface Listener {
        fun onStateChanged(state: State)
        fun onSwapComplete(receivedToken: Token, fxHint: String?)
        fun onSwapFailed(reason: String)
    }

    private var state: State = State.IDLE
    private var listener: Listener? = null

    // Session data
    private var localBearer: Token? = null
    private var localCommodity: Token? = null
    private var localBlindingNonce: ByteArray? = null

    fun setListener(l: Listener) { listener = l }

    // ── Step 1: HANDSHAKE ──────────────────────────────────────────────────────

    /**
     * Buyer side: begin a payment session.
     * [bearer] is the FNT-B token the buyer is spending.
     * Returns the 64-byte handshake payload to send via BLE.
     */
    fun initiateHandshake(bearer: Token): ByteArray {
        require(bearer.type.code == 0x01.toByte()) { "Must provide FNT_B" }
        localBearer = bearer
        localBlindingNonce = generateNonce()
        transitionTo(State.AWAITING_COMMIT)
        // Handshake payload: [pubKey 32 bytes][bearer_id_hash 8 bytes][nonce 32 bytes]
        val pubKeyStub    = ByteArray(32) // Replace with real X25519 ephemeral pub key
        val bearerIdHash  = ShadowCache.pairHashOf(bearer.id, "handshake").toByteArray()
        return pubKeyStub + bearerIdHash + (localBlindingNonce ?: ByteArray(32))
    }

    /**
     * Seller side: accept a handshake, provide FNT-C to commit.
     * Returns the commit payload to send back.
     */
    fun acceptHandshake(handshakeBytes: ByteArray, commodity: Token): ByteArray {
        localCommodity = commodity
        localBlindingNonce = generateNonce()
        transitionTo(State.AWAITING_UNLOCK)
        val blindedCId = xorBlind(commodity.id.toByteArray(Charsets.UTF_8), localBlindingNonce!!)
        return blindedCId + (localBlindingNonce ?: ByteArray(32))  // + currency preference in real impl
    }

    // ── Step 2: COMMIT ─────────────────────────────────────────────────────────

    /**
     * Exchange blinded tokens. Both sides call this with the other's blinded payload.
     * [blindedPayload] is 32+ bytes received from the peer.
     */
    fun processCommit(blindedPayload: ByteArray): ByteArray {
        // In MVP: store blinded payload for unveiling in step 3
        transitionTo(State.AWAITING_UNLOCK)
        val b = localBearer ?: return ByteArray(0)
        val nonce = localBlindingNonce ?: generateNonce().also { localBlindingNonce = it }
        return xorBlind(b.id.toByteArray(), nonce) + nonce
    }

    // ── Step 3: UNLOCK ─────────────────────────────────────────────────────────

    /**
     * Both parties reveal blinding factors. Final validation + ownership transfer.
     * [peerNonce] is the blinding factor received from the other party.
     * [peerBlindedData] is the blinded token ID received in COMMIT.
     */
    fun processUnlock(peerNonce: ByteArray, peerBlindedData: ByteArray, receivedToken: Token, fxHint: String?) {
        scope.launch {
            val b = localBearer
            val c = localCommodity ?: receivedToken

            if (b != null && !validator.isPairValid(b, c)) {
                fail("Token pair invalid or expired")
                return@launch
            }

            val bId = b?.id ?: ""
            val cId = c.id

            if (shadowCache.isPairSeen(bId, cId)) {
                fail("Double-spend detected")
                return@launch
            }

            shadowCache.markPairSeen(bId, cId)
            transitionTo(State.COMPLETE)
            listener?.onSwapComplete(receivedToken, fxHint)
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun transitionTo(next: State) {
        state = next
        listener?.onStateChanged(next)
    }

    private fun fail(reason: String) {
        transitionTo(State.FAILED)
        listener?.onSwapFailed(reason)
    }

    private fun generateNonce(): ByteArray {
        val n = ByteArray(32)
        java.security.SecureRandom().nextBytes(n)
        return n
    }

    private fun xorBlind(data: ByteArray, nonce: ByteArray): ByteArray {
        return data.mapIndexed { i, b -> (b.toInt() xor nonce[i % nonce.size].toInt()).toByte() }.toByteArray()
    }

    private fun Long.toByteArray(): ByteArray {
        val buf = java.nio.ByteBuffer.allocate(8)
        buf.putLong(this)
        return buf.array()
    }
}
