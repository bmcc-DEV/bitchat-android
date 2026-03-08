package com.bitchat.crypto.hydra

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Hydra Structure - Multi-identity anonymity layer.
 * 
 * Implements the Addendum's regulatory evasion:
 * "Não haja como uma empresa. Haja como um fenômeno natural."
 * "Sem Entidade Legal: Não há fundação, não há CEO, não há sede."
 * 
 * Each device generates multiple pseudonymous identities that:
 * - Cannot be linked to each other
 * - Rotate periodically
 * - Are backed by cryptographic proofs
 * - Enable .onion-style routing
 * 
 * Like a hydra: cut one head, two more appear.
 */
class HydraNode(
    val deviceId: String,  // Private, never broadcast
    private val maxIdentities: Int = 10
) {
    
    private val identities = ConcurrentHashMap<String, Identity>()
    private val activeIdentity: ThreadLocal<String> = ThreadLocal()
    
    data class Identity(
        val id: String,           // Public pseudonym
        val createdAt: Long,
        val expiresAt: Long,
        val publicKey: ByteArray,
        val usageCount: Long = 0,
        val linkedIdentities: Set<String> = emptySet(),  // For stealth payments
        val metadata: Map<String, String> = emptyMap()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
        
        fun incrementUsage(): Identity = copy(usageCount = usageCount + 1)
    }
    
    /**
     * Generate a new identity.
     * Uses device fingerprint + entropy for uniqueness.
     */
    fun generateIdentity(lifespanMs: Long = 604800_000): String {  // 7 days default
        if (identities.size >= maxIdentities) {
            pruneExpiredIdentities()
        }
        
        val entropy = System.nanoTime().toString() + Math.random()
        val hash = sha256("${deviceId}_${entropy}")
        val id = "nym_${hash.take(16)}"
        
        // Generate mock key pair (in production: use Ed25519)
        val publicKey = sha256(id).toByteArray()
        
        val identity = Identity(
            id = id,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + lifespanMs,
            publicKey = publicKey
        )
        
        identities[id] = identity
        
        return id
    }
    
    /**
     * Select an identity for use in a transaction.
     * Strategy: use least-used identity that isn't expired.
     */
    fun selectIdentity(): String {
        val valid = identities.values.filter { !it.isExpired() }
        
        if (valid.isEmpty()) {
            // Generate new identity
            return generateIdentity()
        }
        
        // Select least-used
        val selected = valid.minByOrNull { it.usageCount }!!
        identities[selected.id] = selected.incrementUsage()
        activeIdentity.set(selected.id)
        
        return selected.id
    }
    
    /**
     * Link two identities for stealth payments.
     * Allows receiver to prove they control multiple addresses without
     * revealing the link publicly.
     */
    fun linkIdentities(id1: String, id2: String): Boolean {
        val identity1 = identities[id1] ?: return false
        val identity2 = identities[id2] ?: return false
        
        identities[id1] = identity1.copy(
            linkedIdentities = identity1.linkedIdentities + id2
        )
        identities[id2] = identity2.copy(
            linkedIdentities = identity2.linkedIdentities + id1
        )
        
        return true
    }
    
    /**
     * Rotate identity (burn old, generate new).
     * Used when identity is compromised or overused.
     */
    fun rotateIdentity(oldId: String): String {
        identities.remove(oldId)
        return generateIdentity()
    }
    
    /**
     * Prune expired identities.
     */
    private fun pruneExpiredIdentities() {
        val expired = identities.values.filter { it.isExpired() }
        expired.forEach { identities.remove(it.id) }
    }
    
    /**
     * Create an onion-routed path through multiple identities.
     * Each hop only knows previous and next, not source/destination.
     */
    fun createOnionPath(hops: Int = 3): OnionPath {
        val path = mutableListOf<String>()
        
        repeat(hops) {
            path.add(selectIdentity())
        }
        
        return OnionPath(
            pathId = sha256(path.joinToString()),
            hops = path,
            createdAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Verify an onion-routed message at this hop.
     * Returns next hop and decrypted payload.
     */
    fun verifyOnionHop(
        encryptedPayload: ByteArray,
        currentHop: String
    ): OnionHopResult {
        val identity = identities[currentHop] ?: return OnionHopResult(
            success = false,
            error = "Unknown identity"
        )
        
        // In production: decrypt layer using identity's private key
        // For now: simulate decryption
        val decrypted = encryptedPayload  // Placeholder
        
        return OnionHopResult(
            success = true,
            nextHop = "next_hop_id",  // Extracted from decrypted payload
            payload = decrypted
        )
    }
    
    /**
     * Get hydra statistics.
     */
    fun getHydraStats(): HydraStats {
        val totalIdentities = identities.size
        val activeIdentities = identities.values.count { !it.isExpired() }
        val totalUsage = identities.values.sumOf { it.usageCount }
        val averageLifespan = identities.values
            .map { it.expiresAt - it.createdAt }
            .average()
        
        return HydraStats(
            totalIdentities = totalIdentities,
            activeIdentities = activeIdentities,
            expiredIdentities = totalIdentities - activeIdentities,
            totalUsageCount = totalUsage,
            averageLifespanMs = averageLifespan.toLong(),
            maxIdentities = maxIdentities
        )
    }
    
    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    data class OnionPath(
        val pathId: String,
        val hops: List<String>,
        val createdAt: Long
    )
    
    data class OnionHopResult(
        val success: Boolean,
        val nextHop: String? = null,
        val payload: ByteArray? = null,
        val error: String? = null
    )
    
    data class HydraStats(
        val totalIdentities: Int,
        val activeIdentities: Int,
        val expiredIdentities: Int,
        val totalUsageCount: Long,
        val averageLifespanMs: Long,
        val maxIdentities: Int
    )
}
