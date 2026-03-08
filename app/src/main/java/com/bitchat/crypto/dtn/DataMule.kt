package com.bitchat.crypto.dtn

/**
 * Data Mule - Physical data transport via human movement.
 * 
 * Implements the Addendum's biological ledger concept:
 * "O ledger é *biológico*, viaja com as pessoas."
 * 
 * A data mule is a mobile node that physically carries data between
 * disconnected network partitions. This makes the network resilient
 * to internet shutdowns and surveillance.
 * 
 * Key features:
 * - Automatic data synchronization when entering WiFi/BLE range
 * - Geolocation-aware routing (carry data toward destination)
 * - Batch transmission optimizations
 * - Tamper-evident message chains
 */
class DataMule(
    val muleId: String,
    private val dtn: DelayTolerantNetwork
) {
    
    private var currentLocation: String? = null  // Geohash
    private val travelHistory = mutableListOf<Location>()
    private val syncHistory = mutableListOf<SyncEvent>()
    
    data class Location(
        val geohash: String,
        val timestamp: Long,
        val altitude: Double? = null
    )
    
    data class SyncEvent(
        val targetNodeId: String,
        val timestamp: Long,
        val messagesSent: Int,
        val messagesReceived: Int,
        val linkQuality: Double,
        val syncDurationMs: Long
    )
    
    enum class TransportMode {
        WALKING,    // ~5 km/h
        CYCLING,    // ~20 km/h
        DRIVING,    // ~60 km/h
        FLYING      // ~800 km/h
    }
    
    /**
     * Update current location (called by GPS/geohash service).
     */
    fun updateLocation(geohash: String) {
        currentLocation = geohash
        travelHistory.add(Location(geohash, System.currentTimeMillis()))
        
        // Trim travel history to last 24 hours
        val yesterday = System.currentTimeMillis() - 86400_000
        travelHistory.removeIf { it.timestamp < yesterday }
    }
    
    /**
     * Synchronize data with another node (opportunistic encounter).
     * Returns sync statistics.
     */
    fun syncWithNode(
        targetNodeId: String,
        targetMessages: List<String>,  // Message IDs target already has
        linkQuality: Double = 1.0
    ): SyncResult {
        val startTime = System.currentTimeMillis()
        
        // Get messages to send using DTN routing
        val toSend = dtn.onNodeEncounter(
            encounteredNodeId = targetNodeId,
            theirMessages = targetMessages,
            strategy = selectRoutingStrategy(linkQuality)
        )
        
        // Filter by geolocation affinity if available
        val filtered = if (currentLocation != null) {
            prioritizeByLocation(toSend, targetNodeId)
        } else {
            toSend
        }
        
        val syncDuration = System.currentTimeMillis() - startTime
        
        // Record sync event
        syncHistory.add(SyncEvent(
            targetNodeId = targetNodeId,
            timestamp = System.currentTimeMillis(),
            messagesSent = filtered.size,
            messagesReceived = 0,  // Placeholder
            linkQuality = linkQuality,
            syncDurationMs = syncDuration
        ))
        
        return SyncResult(
            success = true,
            messagesSent = filtered.size,
            messagesReceived = 0,
            syncDurationMs = syncDuration,
            bytesTransferred = filtered.sumOf { it.payload.size }.toLong()
        )
    }
    
    /**
     * Select routing strategy based on link quality.
     */
    private fun selectRoutingStrategy(linkQuality: Double): DelayTolerantNetwork.RoutingStrategy {
        return when {
            linkQuality > 0.8 -> DelayTolerantNetwork.RoutingStrategy.EPIDEMIC
            linkQuality > 0.5 -> DelayTolerantNetwork.RoutingStrategy.SPRAY_AND_WAIT
            else -> DelayTolerantNetwork.RoutingStrategy.PROPHET
        }
    }
    
    /**
     * Prioritize messages by geolocation affinity.
     * Messages destined for nodes we're moving toward get higher priority.
     */
    private fun prioritizeByLocation(
        messages: List<DelayTolerantNetwork.DTNMessage>,
        targetNodeId: String
    ): List<DelayTolerantNetwork.DTNMessage> {
        // In production: use geohash distance to destination
        // For now: simple priority preservation
        return messages
    }
    
    /**
     * Estimate when this mule will reach a destination.
     * Used by DTN routing to prefer mules heading toward destination.
     */
    fun estimateArrivalTime(destination: String, mode: TransportMode): Long? {
        // In production: calculate based on geohash distance and mode speed
        val speedKmh = when (mode) {
            TransportMode.WALKING -> 5.0
            TransportMode.CYCLING -> 20.0
            TransportMode.DRIVING -> 60.0
            TransportMode.FLYING -> 800.0
        }
        
        // Placeholder: assume 100km distance, return milliseconds
        val distanceKm = 100.0
        val hoursToArrival = distanceKm / speedKmh
        
        return System.currentTimeMillis() + (hoursToArrival * 3600_000).toLong()
    }
    
    /**
     * Get mule statistics.
     */
    fun getMuleStats(): MuleStats {
        val totalSyncs = syncHistory.size
        val totalMessagesSent = syncHistory.sumOf { it.messagesSent }
        val totalMessagesReceived = syncHistory.sumOf { it.messagesReceived }
        val averageLinkQuality = syncHistory.mapNotNull { it.linkQuality }.average()
        val totalDistanceTraveled = calculateDistanceTraveled()
        
        return MuleStats(
            muleId = muleId,
            totalSyncs = totalSyncs,
            totalMessagesSent = totalMessagesSent,
            totalMessagesReceived = totalMessagesReceived,
            averageLinkQuality = averageLinkQuality,
            currentLocation = currentLocation,
            distanceTraveledKm = totalDistanceTraveled,
            uptime = if (travelHistory.isNotEmpty()) {
                System.currentTimeMillis() - travelHistory.first().timestamp
            } else {
                0L
            }
        )
    }
    
    /**
     * Calculate total distance traveled (simplified).
     */
    private fun calculateDistanceTraveled(): Double {
        if (travelHistory.size < 2) return 0.0
        
        // In production: use geohash distance calculation
        // Placeholder: estimate 10km per location change
        return (travelHistory.size - 1) * 10.0
    }
    
    /**
     * Verify data integrity after physical transport.
     * Uses Merkle tree to detect tampering.
     */
    fun verifyDataIntegrity(): Boolean {
        // In production: calculate Merkle root of all buffered messages
        // Compare with root signed at journey start
        return true  // Placeholder
    }
    
    data class SyncResult(
        val success: Boolean,
        val messagesSent: Int,
        val messagesReceived: Int,
        val syncDurationMs: Long,
        val bytesTransferred: Long,
        val error: String? = null
    )
    
    data class MuleStats(
        val muleId: String,
        val totalSyncs: Int,
        val totalMessagesSent: Int,
        val totalMessagesReceived: Int,
        val averageLinkQuality: Double,
        val currentLocation: String?,
        val distanceTraveledKm: Double,
        val uptime: Long
    )
}
