package com.bitchat.crypto

import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.spec.SecretKeySpec

/**
 * Lightweight key manager for demo environments.
 * Keeps symmetric keys in memory by alias.
 */
object KeyManager {
    private val keys = ConcurrentHashMap<String, SecretKeySpec>()
    private const val DEFAULT_ALIAS = "crypt_high_tech_default"

    fun getOrCreate(alias: String = DEFAULT_ALIAS): SecretKeySpec {
        return keys.getOrPut(alias) {
            val raw = ByteArray(32)
            SecureRandom().nextBytes(raw)
            SecretKeySpec(raw, "AES")
        }
    }
}
