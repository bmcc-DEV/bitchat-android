package com.ghostpay.android.pay.token

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory anti-double-spend cache.
 *
 * Stores 64-bit hashes of token-pair IDs. A pair hash is:
 *   SHA-256(FNT_B.id + FNT_C.id) truncated to 8 bytes → stored as Long.
 *
 * Design goals:
 * - Fast O(1) lookup using ConcurrentHashMap
 * - Max 1,000,000 entries (~8 MB) — entries expire after 48h
 * - No personal data stored — only anonymous pair hashes
 */
class ShadowCache {

    private data class Entry(val hash: Long, val expiryMs: Long)

    // hash → expiryMs (ConcurrentHashMap for thread safety)
    private val store = ConcurrentHashMap<Long, Long>(INITIAL_CAPACITY)

    /**
     * Returns true if this pair hash has already been seen (potential double-spend).
     * Automatically cleans up the specific entry if it has expired.
     */
    fun isSeen(pairHash: Long): Boolean {
        val expiry = store[pairHash] ?: return false
        if (System.currentTimeMillis() > expiry) {
            store.remove(pairHash)
            return false
        }
        return true
    }

    /**
     * Mark a pair hash as seen for 48 hours.
     * If the cache is full, a background sweep evicts expired entries first.
     */
    fun markSeen(pairHash: Long) {
        if (store.size >= MAX_ENTRIES) evictExpired()
        store[pairHash] = System.currentTimeMillis() + TTL_48H
    }

    /** Convenience: compute pair hash from two token IDs and mark as seen. */
    fun markPairSeen(bId: String, cId: String) {
        markSeen(pairHashOf(bId, cId))
    }

    /** Convenience: compute pair hash from two token IDs and check. */
    fun isPairSeen(bId: String, cId: String): Boolean {
        return isSeen(pairHashOf(bId, cId))
    }

    fun size(): Int = store.size

    fun clear() = store.clear()

    // ── Private ────────────────────────────────────────────────────────────────

    private fun evictExpired() {
        val now = System.currentTimeMillis()
        store.entries.removeIf { it.value < now }
    }

    companion object {
        private const val MAX_ENTRIES    = 1_000_000
        private const val INITIAL_CAPACITY = 10_000
        private const val TTL_48H = 48L * 60 * 60 * 1000

        /** SHA-256(bId + cId) → first 8 bytes → Long */
        fun pairHashOf(bId: String, cId: String): Long {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(bId.toByteArray() + cId.toByteArray())
            var result = 0L
            for (i in 0 until 8) result = (result shl 8) or (digest[i].toLong() and 0xFF)
            return result
        }
    }
}
