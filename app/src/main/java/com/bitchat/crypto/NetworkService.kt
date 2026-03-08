package com.bitchat.crypto

/**
 * Simple in-memory network service stub to simulate peer-to-peer message propagation.
 * In a real system this would use sockets, gRPC, or a blockchain gossip protocol.
 */
object NetworkService {
    private val listeners = mutableListOf<(String) -> Unit>()
    private val orderedListeners = mutableListOf<(NetworkMessage) -> Unit>()
    private val seenIds = linkedSetOf<String>()
    private var seq = 0L

    data class NetworkMessage(
        val id: String,
        val sequence: Long,
        val payload: String
    )

    /**
     * Register a listener that receives messages sent over the network.
     */
    fun registerListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun registerOrderedListener(listener: (NetworkMessage) -> Unit) {
        orderedListeners.add(listener)
    }

    /**
     * Broadcast a message to all registered peers.
     */
    fun broadcast(message: String) {
        val next = NetworkMessage(
            id = "msg-${++seq}",
            sequence = seq,
            payload = message
        )
        broadcastOrdered(next)
    }

    fun broadcastOrdered(message: NetworkMessage) {
        if (!seenIds.add(message.id)) return
        listeners.forEach { it(message.payload) }
        orderedListeners.forEach { it(message) }
        if (seenIds.size > 10_000) {
            // keep memory bounded
            seenIds.clear()
        }
    }
}
