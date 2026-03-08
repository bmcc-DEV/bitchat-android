package com.bitchat.crypto

/**
 * Very basic encryption service stub. In reality this would perform
 * fully-homomorphic encryption; here we just reverse the byte array.
 */
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionService {
    // AES key (128-bit) hardcoded for demonstration
    private val keyBytes = ByteArray(16).apply { SecureRandom().nextBytes(this) }
    private val keySpec = SecretKeySpec(keyBytes, "AES")

    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(data)
        return iv + encrypted // prepend IV
    }

    fun decrypt(data: ByteArray): ByteArray {
        if (data.size < 16) return ByteArray(0)
        val iv = data.copyOfRange(0, 16)
        val cipherData = data.copyOfRange(16, data.size)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
        return cipher.doFinal(cipherData)
    }

    /**
     * Homomorphic addition: encrypts two integers and adds ciphertext
     * by decrypting, adding and re-encrypting (not true homomorphic!).
     */
    fun homomorphicAdd(encA: ByteArray, encB: ByteArray): ByteArray {
        val a = String(decrypt(encA)).toDouble()
        val b = String(decrypt(encB)).toDouble()
        val sum = a + b
        return encrypt(sum.toString().toByteArray())
    }
}
