package com.bitchat.crypto.crdt

import org.junit.Assert.*
import org.junit.Test

class VectorClockTest {
    
    @Test
    fun `increment increases timestamp for node`() {
        val clock = VectorClock.empty()
        val incremented = clock.increment("node1")
        
        assertEquals(1L, incremented.get("node1"))
        assertEquals(0L, incremented.get("node2"))
    }

    @Test
    fun `merge takes max of each component`() {
        val clock1 = VectorClock.empty()
            .increment("node1")
            .increment("node1")
        val clock2 = VectorClock.empty()
            .increment("node1")
            .increment("node2")
        
        val merged = clock1.merge(clock2)
        
        assertEquals(2L, merged.get("node1"))
        assertEquals(1L, merged.get("node2"))
    }

    @Test
    fun `merge is commutative`() {
        val clock1 = VectorClock.empty().increment("node1")
        val clock2 = VectorClock.empty().increment("node2")
        
        val merged1 = clock1.merge(clock2)
        val merged2 = clock2.merge(clock1)
        
        assertEquals(merged1.get("node1"), merged2.get("node1"))
        assertEquals(merged1.get("node2"), merged2.get("node2"))
    }

    @Test
    fun `merge is idempotent`() {
        val clock = VectorClock.empty().increment("node1")
        val merged = clock.merge(clock)
        
        assertEquals(clock.get("node1"), merged.get("node1"))
    }

    @Test
    fun `happenedBefore detects causal ordering`() {
        val clock1 = VectorClock.empty().increment("node1")
        val clock2 = clock1.increment("node1")
        
        assertTrue(clock1.happenedBefore(clock2))
        assertFalse(clock2.happenedBefore(clock1))
    }

    @Test
    fun `concurrent clocks are detected`() {
        val clock1 = VectorClock.empty().increment("node1")
        val clock2 = VectorClock.empty().increment("node2")
        
        assertTrue(clock1.isConcurrentWith(clock2))
        assertTrue(clock2.isConcurrentWith(clock1))
    }

    @Test
    fun `toString and fromString roundtrip`() {
        val clock = VectorClock.empty()
            .increment("alice")
            .increment("alice")
            .increment("bob")
        
        val str = clock.toString()
        val parsed = VectorClock.fromString(str)
        
        assertEquals(clock.get("alice"), parsed.get("alice"))
        assertEquals(clock.get("bob"), parsed.get("bob"))
    }
}
