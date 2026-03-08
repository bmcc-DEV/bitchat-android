package com.bitchat.crypto

/**
 * Minimal deterministic ordering/consensus helper for incoming gossip messages.
 */
object ConsensusEngine {
    fun order(messages: List<NetworkService.NetworkMessage>): List<NetworkService.NetworkMessage> {
        return messages.sortedWith(compareBy<NetworkService.NetworkMessage> { it.sequence }.thenBy { it.id })
    }

    fun detectCollisions(messages: List<NetworkService.NetworkMessage>): List<String> {
        val seenSeq = mutableSetOf<Long>()
        return messages.filter { !seenSeq.add(it.sequence) }.map { it.id }
    }
}
