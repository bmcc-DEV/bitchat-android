package com.ghostpay.android.pay.mesh

/**
 * Pay protocol packet type codes.
 * Extend the existing bitchat MessageType byte space (ghost range: 0x20–0x2F).
 */
object PayMessageType {
    const val PAY_HANDSHAKE: Byte = 0x20  // Key exchange + currency preference
    const val PAY_COMMIT:    Byte = 0x21  // Blinded B+C tokens exchanged
    const val PAY_UNLOCK:    Byte = 0x22  // Blinding factors revealed; ownership transferred
    const val PAY_BURN:      Byte = 0x23  // FNT_D mint announcement (burn proof)
    const val PAY_STATE_VEC: Byte = 0x24  // Anonymous gossip state vector broadcast
}
