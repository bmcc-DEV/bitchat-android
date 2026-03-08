package com.bitchat.crypto.dtn

import com.bitchat.crypto.crdt.VectorClock
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.exp

/**
 * Delay-Tolerant Network (DTN) - Data Muling for Mesh Resilience.
 * 
 * Implements the Addendum's solution:
 * "O ledger é *biológico*, viaja com as pessoas. Um nó em Fortaleza 
 *  carrega transações no seu buffer e as 'cospe' para a rede quando 
 *  encontra um nó em São Paulo."
 * 
 * Based on RFC 4838 (Delay-Tolerant Networking Architecture).
 * 
 * Key features:
 * - Store-and-forward message buffering
 * - Opportunistic routing (spray-and-wait)
 * - Epidemic dissemination for critical messages
 * - TTL-based expiration
 * - Priority-based eviction
 */
class DelayTolerantNetwork {
    
    private val messageBuffer = ConcurrentHashMap<String, DTNMessage>()
    private val routingTable = ConcurrentHashMap<String, MutableSet<String>>()  // destination -> knownRoutes
    private val encounterHistory = ConcurrentHashMap<String, MutableList<Encounter>>()
    
    private val maxBufferSize = 1000  // Max messages per node
    private val defaultTTL = 604800_000L  // 7 days in milliseconds
    
    data class DTNMessage(
        val id: String,
        val source: String,
        val destination: String,
        val payload: ByteArray,
        val priority: Priority,
        val ttl: Long,
        val createdAt: Long,
        val vectorClock: VectorClock,
        val hopCount: Int = 0,
        val routingMetadata: Map<String, String> = emptyMap()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > createdAt + ttl
        
        fun incrementHop(): DTNMessage = copy(hopCount = hopCount + 1)
    }
    
    enum class Priority {
        CRITICAL,   // Network consensus, fraud alerts
        HIGH,       // Financial transactions
        NORMAL,     // Chat messages
        LOW         // Bulk data, metadata
    }
    
    data class Encounter(
        val nodeId: String,
        val timestamp: Long,
        val location: String? = null,  // Geohash for location-based routing
        val linkQuality: Double = 1.0
    )
    
    enum class RoutingStrategy {
        EPIDEMIC,       // Flood to all neighbors
        SPRAY_AND_WAIT, // Limited copies
        PROPHET,        // Probabilistic routing using encounter history
        DIRECT          // Only forward to destination
    }
    
    /**
     * Store a message in the local buffer.
     */
    fun storeMessage(message: DTNMessage): Boolean {
        if (messageBuffer.size >= maxBufferSize) {
            evictLowPriorityMessages()
        }
        
        messageBuffer[message.id] = message
        return true
    }
    
    /**
     * Forward messages upon encounter with another node.
     * Returns list of messages to transmit.
     */
    fun onNodeEncounter(
        encounteredNodeId: String,
        theirMessages: List<String>,  // Message IDs they already have
        strategy: RoutingStrategy = RoutingStrategy.PROPHET
    ): List<DTNMessage> {
        // Record encounter for routing probability
        recordEncounter(encounteredNodeId)
        
        val toForward = mutableListOf<DTNMessage>()
        
        for ((id, message) in messageBuffer) {
            if (message.isExpired()) continue
            if (id in theirMessages) continue  // They already have it
            
            val shouldForward = when (strategy) {
                RoutingStrategy.EPIDEMIC -> true
                RoutingStrategy.SPRAY_AND_WAIT -> message.hopCount < 10
                RoutingStrategy.PROPHET -> shouldForwardProphet(message, encounteredNodeId)
                RoutingStrategy.DIRECT -> message.destination == encounteredNodeId
            }
            
            if (shouldForward) {
                toForward.add(message.incrementHop())
            }
        }
        
        // Sort by priority
        return toForward.sortedByDescending { it.priority.ordinal }
    }
    
    /**
     * PRoPHET (Probabilistic Routing Protocol using History).
     * Calculates delivery probability based on encounter history.
     * 
     * Formula:
     *   P(a,b) = P_init if encounter
     *   P(a,b) = P(a,b)_old × γ^k (aging)
     *   P(a,c) = P(a,c)_old + (1 - P(a,c)_old) × P(a,b) × P(b,c) × β (transitivity)
     */
    private fun shouldForwardProphet(message: DTNMessage, encounteredNode: String): Boolean {
        val deliveryProb = calculateDeliveryProbability(message.destination, encounteredNode)
        val myProb = calculateDeliveryProbability(message.destination, "self")
        
        // Forward if encountered node has better probability
        return deliveryProb > myProb
    }
    
    private fun calculateDeliveryProbability(destination: String, intermediary: String): Double {
        val encounters = encounterHistory[intermediary] ?: return 0.1
        
        // Check direct encounter history
        val recentEncounters = encounters.filter { 
            System.currentTimeMillis() - it.timestamp < 86400_000  // Last 24h
        }
        
        if (recentEncounters.any { it.nodeId == destination }) {
            return 0.9  // High probability if recent direct encounter
        }
        
        // Calculate transitivity
        val P_init = 0.75
        val gamma = 0.98  // Aging factor
        val beta = 0.25   // Transitivity scaling
        
        // Simplified: use encounter frequency
        val frequency = encounters.count { it.nodeId == destination }.toDouble() / 
                       (encounters.size + 1)
        
        return P_init * frequency * exp(-0.1 * (encounters.size - 1))
    }
    
    /**
     * Record an encounter for PRoPHET routing.
     */
    private fun recordEncounter(nodeId: String) {
        encounterHistory.computeIfAbsent(nodeId) { mutableListOf() }
            .add(Encounter(
                nodeId = nodeId,
                timestamp = System.currentTimeMillis()
            ))
        
        // Maintain routing table
        routingTable.computeIfAbsent(nodeId) { mutableSetOf() }.add(nodeId)
    }
    
    /**
     * Receive messages from another node.
     */
    fun receiveMessages(messages: List<DTNMessage>) {
        for (message in messages) {
            if (message.destination == "self") {
                // Message reached destination - deliver it
                deliverMessage(message)
            } else {
                // Store for forwarding
                storeMessage(message)
            }
        }
    }
    
    /**
     * Deliver message to local application.
     */
    private fun deliverMessage(message: DTNMessage) {
        // In production: trigger callback to application layer
        messageBuffer.remove(message.id)
    }
    
    /**
     * Evict low-priority messages when buffer is full.
     * Strategy: LRU + priority-based.
     */
    private fun evictLowPriorityMessages() {
        val candidates = messageBuffer.values
            .sortedWith(compareBy(
                { it.priority.ordinal },
                { it.createdAt }
            ))
        
        // Remove bottom 20%
        val toRemove = (maxBufferSize * 0.2).toInt()
        candidates.take(toRemove).forEach { 
            messageBuffer.remove(it.id) 
        }
    }
    
    /**
     * Clean expired messages.
     */
    fun cleanExpiredMessages() {
        val expired = messageBuffer.values.filter { it.isExpired() }
        expired.forEach { messageBuffer.remove(it.id) }
    }
    
    /**
     * Get buffer statistics.
     */
    fun getBufferStats(): BufferStats {
        val totalMessages = messageBuffer.size
        val byPriority = messageBuffer.values.groupBy { it.priority }
        val averageHops = messageBuffer.values.map { it.hopCount }.average()
        val oldestMessage = messageBuffer.values.minByOrNull { it.createdAt }
        
        return BufferStats(
            totalMessages = totalMessages,
            criticalMessages = byPriority[Priority.CRITICAL]?.size ?: 0,
            highMessages = byPriority[Priority.HIGH]?.size ?: 0,
            normalMessages = byPriority[Priority.NORMAL]?.size ?: 0,
            lowMessages = byPriority[Priority.LOW]?.size ?: 0,
            averageHopCount = averageHops,
            oldestMessageAge = oldestMessage?.let { 
                System.currentTimeMillis() - it.createdAt 
            } ?: 0L,
            bufferUtilization = totalMessages.toDouble() / maxBufferSize
        )
    }
    
    /**
     * Get routing statistics.
     */
    fun getRoutingStats(): RoutingStats {
        val totalRoutes = routingTable.size
        val encounterCount = encounterHistory.values.sumOf { it.size }
        val recentEncounters = encounterHistory.values
            .flatMap { it }
            .count { System.currentTimeMillis() - it.timestamp < 3600_000 }  // Last hour
        
        return RoutingStats(
            knownDestinations = totalRoutes,
            totalEncounters = encounterCount,
            recentEncounters = recentEncounters,
            routingTableSize = routingTable.values.sumOf { it.size }
        )
    }
    
    data class BufferStats(
        val totalMessages: Int,
        val criticalMessages: Int,
        val highMessages: Int,
        val normalMessages: Int,
        val lowMessages: Int,
        val averageHopCount: Double,
        val oldestMessageAge: Long,
        val bufferUtilization: Double
    )
    
    data class RoutingStats(
        val knownDestinations: Int,
        val totalEncounters: Int,
        val recentEncounters: Int,
        val routingTableSize: Int
    )
}
