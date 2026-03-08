package com.bitchat.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore

/**
 * Android Keystore-backed AES-GCM key manager.
 * Uses hardware-backed keys where available.
 */
object AndroidKeyStoreKeyManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    private fun getOrCreate(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setUnlockedDeviceRequired(false)
        }

        generator.init(builder.build())
        return generator.generateKey()
    }

    fun encrypt(alias: String, plain: ByteArray): ByteArray {
        val key = getOrCreate(alias)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain)
        return iv + encrypted
    }

    fun decrypt(alias: String, payload: ByteArray): ByteArray {
        if (payload.size <= IV_BYTES) return ByteArray(0)
        val key = getOrCreate(alias)
        val iv = payload.copyOfRange(0, IV_BYTES)
        val body = payload.copyOfRange(IV_BYTES, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(body)
    }
}
