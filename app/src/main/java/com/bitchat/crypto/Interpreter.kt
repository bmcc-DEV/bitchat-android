package com.bitchat.crypto

import java.util.Base64

/**
 * A stub for the cryptographic interpreter (NewtonCider / Objecider).
 * It would execute encrypted transactions and apply FHE.
 */
object Interpreter {
    data class CryptoTransaction(val payload: ByteArray)

    // simple ledger instance used during execution
    private var ledger = CryptoLedger()
    private var suppressNextInbound = false

    init {
        // register to receive messages from the network
        NetworkService.registerListener { msg ->
            if (suppressNextInbound) {
                suppressNextInbound = false
                return@registerListener
            }
            executeInternal(msg.toByteArray(), propagate = false)
        }
    }

    fun execute(transaction: CryptoTransaction) {
        executeInternal(transaction.payload, propagate = true)
    }

    private fun executeInternal(rawPayload: ByteArray, propagate: Boolean) {
        val rawText = String(rawPayload)
        val clearText = if (rawText.startsWith("enc:")) {
            val cipherBytes = Base64.getDecoder().decode(rawText.removePrefix("enc:"))
            String(EncryptionService.decrypt(cipherBytes))
        } else {
            rawText
        }

        println("Executing cryptographic transaction (stub)")
        if (clearText.isNotEmpty()) {
            val parts = clearText.split(":")
            if (parts.size == 3) {
                val from = parts[0]
                val to = parts[1]
                val amount = parts[2].toDoubleOrNull() ?: 0.0
                ledger.transfer(from, to, amount)
            }
        }

        if (propagate) {
            val encrypted = EncryptionService.encrypt(clearText.toByteArray())
            val envelope = "enc:" + Base64.getEncoder().encodeToString(encrypted)
            suppressNextInbound = true
            NetworkService.broadcast(envelope)
        }
    }

    fun getBalance(account: String): Double = ledger.getBalance(account)

    // helper for unit tests and initialization
    fun deposit(account: String, amount: Double) {
        ledger.deposit(account, amount)
    }

    fun resetState() {
        ledger = CryptoLedger()
        suppressNextInbound = false
    }
}
