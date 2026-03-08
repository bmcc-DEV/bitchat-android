package com.bitchat.crypto.topology

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TopologyGuardianTest {
    
    private lateinit var guardian: TopologyGuardian
    
    @Before
    fun setup() {
        guardian = TopologyGuardian(resilienceThreshold = 0.5)
    }
    
    @Test
    fun `adding nodes updates graph`() {
        guardian.addNode("node1")
        guardian.addNode("node2")
        
        val robustness = guardian.calculateRobustness()
        assertEquals(2, robustness.connectedComponents)
    }
    
    @Test
    fun `adding edges connects nodes`() {
        guardian.addNode("node1")
        guardian.addNode("node2")
        guardian.addEdge("node1", "node2")
        
        val robustness = guardian.calculateRobustness()
        assertEquals(1, robustness.connectedComponents)
    }
    
    @Test
    fun `removing edge creates partition`() {
        guardian.addNode("node1")
        guardian.addNode("node2")
        guardian.addEdge("node1", "node2")
        
        guardian.removeEdge("node1", "node2")
        
        val robustness = guardian.calculateRobustness()
        assertEquals(2, robustness.connectedComponents)
    }
    
    @Test
    fun `betti0 counts connected components`() {
        guardian.addNode("node1")
        guardian.addNode("node2")
        guardian.addNode("node3")
        
        guardian.addEdge("node1", "node2")
        // node3 is isolated
        
        assertEquals(2, guardian.calculateBetti0())
    }
    
    @Test
    fun `betti1 counts independent cycles`() {
        // Create triangle (1 cycle)
        guardian.addNode("node1")
        guardian.addNode("node2")
        guardian.addNode("node3")
        
        guardian.addEdge("node1", "node2")
        guardian.addEdge("node2", "node3")
        guardian.addEdge("node3", "node1")
        
        val beta1 = guardian.calculateBetti1()
        assertTrue(beta1 >= 1) // At least one cycle
    }
    
    @Test
    fun `detect partitions in disconnected graph`() {
        guardian.addNode("node1")
        guardian.addNode("node2")
        guardian.addNode("node3")
        guardian.addNode("node4")
        
        guardian.addEdge("node1", "node2")
        guardian.addEdge("node3", "node4")
        // Two separate components
        
        val partitions = guardian.detectPartitions()
        assertEquals(2, partitions.size)
    }
    
    @Test
    fun `heal partitions adds bridge edges`() {
        guardian.addNode("node1")
        guardian.addNode("node2")
        guardian.addNode("node3")
        guardian.addNode("node4")
        
        guardian.addEdge("node1", "node2")
        guardian.addEdge("node3", "node4")
        
        val bridges = guardian.healPartitions()
        
        assertFalse(bridges.isEmpty())
        assertEquals(1, guardian.calculateBetti0()) // Now connected
    }
    
    @Test
    fun `reconfigure mesh increases connectivity`() {
        // Create sparse network
        for (i in 1..5) {
            guardian.addNode("node$i")
        }
        
        guardian.addEdge("node1", "node2")
        guardian.addEdge("node2", "node3")
        
        val edgesAdded = guardian.reconfigureMeshTopology(targetRedundancy = 2)
        
        assertTrue(edgesAdded > 0)
        
        val robustness = guardian.calculateRobustness()
        assertTrue(robustness.averageDegree >= 2.0)
    }
    
    @Test
    fun `resilience check passes for well-connected graph`() {
        // Create fully connected mesh (5 nodes)
        for (i in 1..5) {
            guardian.addNode("node$i")
        }
        
        for (i in 1..5) {
            for (j in i+1..5) {
                guardian.addEdge("node$i", "node$j")
            }
        }
        
        assertTrue(guardian.isResilient())
    }
    
    @Test
    fun `attack simulation measures robustness`() {
        // Create mesh network
        for (i in 1..10) {
            guardian.addNode("node$i")
        }
        
        // Ring topology
        for (i in 1..9) {
            guardian.addEdge("node$i", "node${i+1}")
        }
        guardian.addEdge("node10", "node1")
        
        val simulation = guardian.simulateAttack(attackPercentage = 0.3)
        
        assertEquals(3, simulation.nodesRemoved)
        assertEquals(10, simulation.totalNodes)
        assertEquals(0.7, simulation.survivalRate, 0.01)
    }
    
    @Test
    fun `euler characteristic reflects connectivity`() {
        guardian.addNode("node1")
        guardian.addNode("node2")
        guardian.addNode("node3")
        
        val chi1 = guardian.calculateEulerCharacteristic()
        
        guardian.addEdge("node1", "node2")
        guardian.addEdge("node2", "node3")
        guardian.addEdge("node3", "node1")
        
        val chi2 = guardian.calculateEulerCharacteristic()
        
        // More edges → more connected → different χ
        assertNotEquals(chi1, chi2, 0.001)
    }
    
    @Test
    fun `robustness score aggregates metrics`() {
        for (i in 1..5) {
            guardian.addNode("node$i")
        }
        
        guardian.addEdge("node1", "node2")
        guardian.addEdge("node2", "node3")
        guardian.addEdge("node3", "node4")
        guardian.addEdge("node4", "node5")
        guardian.addEdge("node5", "node1") // Create cycle
        
        val score = guardian.calculateRobustness()
        
        assertEquals(1, score.connectedComponents)
        assertTrue(score.independentCycles >= 1)
        assertTrue(score.averageDegree >= 2.0)
    }
}
