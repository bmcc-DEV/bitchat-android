package com.bitchat.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

object WalletAuthManager {
    private var pinHash: String? = null
    private var salt: ByteArray? = null
    private var sessionToken: String? = null

    fun registerPin(pin: String) {
        require(pin.length >= 4) { "pin too short" }
        val newSalt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        salt = newSalt
        pinHash = hash(pin, newSalt)
    }

    fun authenticate(pin: String): Boolean {
        val s = salt ?: return false
        val expected = pinHash ?: return false
        val ok = hash(pin, s) == expected
        if (ok) {
            sessionToken = UUID.randomUUID().toString()
            CryptoMetrics.inc("wallet.auth.success")
        } else {
            CryptoMetrics.inc("wallet.auth.failure")
        }
        return ok
    }

    fun currentSessionToken(): String? = sessionToken

    private fun hash(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val bytes = digest.digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
