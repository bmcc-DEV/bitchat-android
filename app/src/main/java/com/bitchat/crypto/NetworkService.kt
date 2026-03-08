package com.bitchat.crypto

/**
 * Simple in-memory network service stub to simulate peer-to-peer message propagation.
 * In a real system this would use sockets, gRPC, or a blockchain gossip protocol.
 */
object NetworkService {
    private val listeners = mutableListOf<(String) -> Unit>()

    /**
     * Register a listener that receives messages sent over the network.
     */
    fun registerListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    /**
     * Broadcast a message to all registered peers.
     */
    fun broadcast(message: String) {
        listeners.forEach { it(message) }
    }
}
