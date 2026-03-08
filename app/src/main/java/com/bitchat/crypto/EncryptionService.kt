package com.bitchat.crypto

/**
 * Very basic encryption service stub. In reality this would perform
 * fully-homomorphic encryption; here we just reverse the byte array.
 */
object EncryptionService {
    fun encrypt(data: ByteArray): ByteArray {
        return data.reversedArray()
    }

    fun decrypt(data: ByteArray): ByteArray {
        return data.reversedArray()
    }
}
