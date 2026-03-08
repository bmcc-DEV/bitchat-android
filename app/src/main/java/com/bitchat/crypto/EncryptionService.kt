package com.bitchat.crypto

/**
 * Very basic encryption service stub. In reality this would perform
 * fully-homomorphic encryption; here we just reverse the byte array.
 */
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

object EncryptionService {
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    fun encrypt(data: ByteArray, keyAlias: String = "crypt_high_tech_default"): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(IV_BYTES).apply { SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, KeyManager.getOrCreate(keyAlias), GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    fun decrypt(data: ByteArray, keyAlias: String = "crypt_high_tech_default"): ByteArray {
        if (data.size <= IV_BYTES) return ByteArray(0)
        val iv = data.copyOfRange(0, IV_BYTES)
        val cipherData = data.copyOfRange(IV_BYTES, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, KeyManager.getOrCreate(keyAlias), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherData)
    }
}
