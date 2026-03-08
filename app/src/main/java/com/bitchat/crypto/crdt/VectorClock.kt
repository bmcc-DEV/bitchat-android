package com.bitchat.crypto.crdt

import java.util.concurrent.ConcurrentHashMap

/**
 * Vector Clock for causal ordering in distributed systems.
 * Part of CRDT infrastructure for the Web-OS holograma P2P.
 */
data class VectorClock(
    private val clock: ConcurrentHashMap<String, Long> = ConcurrentHashMap()
) {
    /**
     * Increment the clock for this node.
     */
    fun increment(nodeId: String): VectorClock {
        val newClock = ConcurrentHashMap(clock)
        newClock[nodeId] = (newClock[nodeId] ?: 0L) + 1L
        return VectorClock(newClock)
    }

    /**
     * Get the timestamp for a specific node.
     */
    fun get(nodeId: String): Long = clock[nodeId] ?: 0L

    /**
     * Merge two vector clocks (max of each component).
     * This is the ⊔ operator for the join semi-lattice.
     */
    fun merge(other: VectorClock): VectorClock {
        val mergedClock = ConcurrentHashMap(clock)
        other.clock.forEach { (node, timestamp) ->
            mergedClock[node] = maxOf(mergedClock[node] ?: 0L, timestamp)
        }
        return VectorClock(mergedClock)
    }

    /**
     * Check if this clock happened before another (causally).
     * Returns true if this ≤ other (all components less than or equal).
     */
    fun happenedBefore(other: VectorClock): Boolean {
        val allNodes = (clock.keys + other.clock.keys).toSet()
        var strictlyLess = false
        
        for (node in allNodes) {
            val thisTime = get(node)
            val otherTime = other.get(node)
            
            if (thisTime > otherTime) return false
            if (thisTime < otherTime) strictlyLess = true
        }
        
        return strictlyLess
    }

    /**
     * Check if two clocks are concurrent (neither happened before the other).
     */
    fun isConcurrentWith(other: VectorClock): Boolean {
        return !happenedBefore(other) && !other.happenedBefore(this)
    }

    override fun toString(): String = clock.entries
        .sortedBy { it.key }
        .joinToString(",") { "${it.key}:${it.value}" }

    companion object {
        fun empty(): VectorClock = VectorClock()
        
        fun fromString(str: String): VectorClock {
            val clock = ConcurrentHashMap<String, Long>()
            if (str.isNotEmpty()) {
                str.split(",").forEach { entry ->
                    val (node, time) = entry.split(":")
                    clock[node] = time.toLong()
                }
            }
            return VectorClock(clock)
        }
    }
}
