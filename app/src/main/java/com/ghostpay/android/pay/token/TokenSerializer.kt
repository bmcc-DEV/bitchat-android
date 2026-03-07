package com.ghostpay.android.pay.token

import java.nio.ByteBuffer

/**
 * Compact binary serialisation of a token pair (FNT-B + FNT-C) into ≤ 512 bytes
 * suitable for embedding in a BLE advertisement payload or NFC NDEF record.
 *
 * Wire format (little-endian):
 *
 *  [1] magic  = 0xFB              (Fantasma Block)
 *  [1] version = 0x01
 *  [1] type_b  (FNT_B code byte)
 *  [1] type_c  (FNT_C code byte)
 *  [2] id_len  (total bytes for both IDs)
 *  [n] bearer_id  (UTF-8, max 32 bytes)
 *  [n] commodity_id
 *  [8] amount  (Double, 8 bytes)
 *  [1] unit_len
 *  [n] unit    (UTF-8)
 *  [1] commodity_len
 *  [n] commodity_name
 *  [8] expiry_bearer   (Long, epoch ms)
 *  [8] expiry_commodity
 * [32] sig_bearer      (HMAC-SHA256 of bearer canonical bytes)
 * [32] sig_commodity
 *
 * Total typically ≤ 200 bytes for typical commodities.
 */
object TokenSerializer {

    private const val MAGIC: Byte = 0xFB.toByte()
    private const val VERSION: Byte = 0x01

    fun encode(bearer: Token, commodity: Token): ByteArray {
        val bearerId  = bearer.id.toByteArray(Charsets.UTF_8)
        val commodId  = commodity.id.toByteArray(Charsets.UTF_8)
        val unitBytes = bearer.unit.toByteArray(Charsets.UTF_8)
        val commBytes = bearer.commodity.toByteArray(Charsets.UTF_8)

        val buf = ByteBuffer.allocate(512)
        buf.put(MAGIC)
        buf.put(VERSION)
        buf.put(bearer.type.code)
        buf.put(commodity.type.code)

        // IDs
        buf.put(bearerId.size.toByte())
        buf.put(bearerId)
        buf.put(commodId.size.toByte())
        buf.put(commodId)

        // Amount (shared quantity)
        buf.putDouble(bearer.amount)

        // Unit and commodity name
        buf.put(unitBytes.size.toByte())
        buf.put(unitBytes)
        buf.put(commBytes.size.toByte())
        buf.put(commBytes)

        // Expiries
        buf.putLong(bearer.expiryMs)
        buf.putLong(commodity.expiryMs)

        // Signatures (truncated to 32 bytes each)
        buf.put(bearer.issuerSig.take(32).toByteArray())
        buf.put(commodity.issuerSig.take(32).toByteArray())

        val result = ByteArray(buf.position())
        System.arraycopy(buf.array(), 0, result, 0, result.size)
        return result
    }

    data class DecodedPair(val bearer: Token, val commodity: Token)

    fun decode(bytes: ByteArray, ownerPubKey: ByteArray): DecodedPair? {
        return try {
            val buf = ByteBuffer.wrap(bytes)
            if (buf.get() != MAGIC) return null
            if (buf.get() != VERSION) return null

            val typeB = TokenType.fromCode(buf.get()) ?: return null
            val typeC = TokenType.fromCode(buf.get()) ?: return null

            val bIdLen = buf.get().toInt() and 0xFF
            val bId    = ByteArray(bIdLen).also { buf.get(it) }.toString(Charsets.UTF_8)
            val cIdLen = buf.get().toInt() and 0xFF
            val cId    = ByteArray(cIdLen).also { buf.get(it) }.toString(Charsets.UTF_8)

            val amount = buf.double

            val unitLen  = buf.get().toInt() and 0xFF
            val unit     = ByteArray(unitLen).also { buf.get(it) }.toString(Charsets.UTF_8)
            val commLen  = buf.get().toInt() and 0xFF
            val commodity = ByteArray(commLen).also { buf.get(it) }.toString(Charsets.UTF_8)

            val expiryB = buf.long
            val expiryC = buf.long

            val sigB = ByteArray(32).also { buf.get(it) }
            val sigC = ByteArray(32).also { buf.get(it) }

            val bearer = Token(bId, typeB, amount, unit, commodity, expiryB, ownerPubKey, sigB)
            val comm   = Token(cId, typeC, amount, unit, commodity, expiryC, ownerPubKey, sigC)
            DecodedPair(bearer, comm)
        } catch (_: Exception) {
            null
        }
    }
}
