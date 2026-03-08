package com.bitchat.crypto

import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Simple key management: generates and holds symmetric and asymmetric keys.
 */
object KeyManagementService {
    private val symmetricKeys = mutableMapOf<String, SecretKey>()
    private val asymmetricKeys = mutableMapOf<String, Pair<PublicKey, PrivateKey>>()

    fun generateSymmetricKey(id: String): SecretKey {
        val keyBytes = ByteArray(16).apply { java.security.SecureRandom().nextBytes(this) }
        val key = SecretKeySpec(keyBytes, "AES")
        symmetricKeys[id] = key
        return key
    }

    fun getSymmetricKey(id: String): SecretKey? = symmetricKeys[id]

    fun generateAsymmetricKeyPair(id: String) {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()
        asymmetricKeys[id] = Pair(kp.public, kp.private)
    }

    fun getPublicKey(id: String): PublicKey? = asymmetricKeys[id]?.first
    fun getPrivateKey(id: String): PrivateKey? = asymmetricKeys[id]?.second
}
