package com.bitchat.crypto

import java.util.Base64
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * Independent node with local ledger state and ordered gossip processing.
 */
class DistributedNode(
    val nodeId: String = UUID.randomUUID().toString()
) {
    private val ledger = CryptoLedger()
    private val seq = AtomicLong(0)
    private val inbox = CopyOnWriteArrayList<NetworkService.NetworkMessage>()

    fun attach() {
        NetworkService.registerOrderedListener { msg ->
            inbox.add(msg)
            processInbox()
        }
    }

    fun deposit(account: String, amount: Double) {
        ledger.deposit(account, amount)
    }

    fun submitTransfer(from: String, to: String, amount: Double) {
        val clear = "$from:$to:$amount".toByteArray()
        val encrypted = EncryptionService.encrypt(clear)
        val envelope = "enc:" + Base64.getEncoder().encodeToString(encrypted)
        val localSeq = seq.incrementAndGet()
        NetworkService.broadcastOrdered(
            NetworkService.NetworkMessage(
                id = "$nodeId-$localSeq",
                sequence = localSeq,
                payload = envelope
            )
        )
    }

    fun balance(account: String): Double = ledger.getBalance(account)

    private fun processInbox() {
        val ordered = ConsensusEngine.order(inbox.toList())
        val collisions = ConsensusEngine.detectCollisions(ordered)
        if (collisions.isNotEmpty()) {
            CryptoMetrics.inc("network.collisions", collisions.size.toLong())
        }

        ordered.forEach { msg ->
            val clearText = if (msg.payload.startsWith("enc:")) {
                val body = msg.payload.removePrefix("enc:")
                val cipher = Base64.getDecoder().decode(body)
                String(EncryptionService.decrypt(cipher))
            } else {
                msg.payload
            }

            val p = clearText.split(":")
            if (p.size == 3) {
                val amount = p[2].toDoubleOrNull() ?: return@forEach
                ledger.transfer(p[0], p[1], amount)
            }
        }

        inbox.clear()
    }
}
