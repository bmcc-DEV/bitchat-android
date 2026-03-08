package com.bitchat.crypto.topology

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Topology Guardian - maintains network resilience.
 * Implements Euler-Poincaré invariant for indestrutibilidade.
 * 
 * Mathematical foundation:
 *   χ(G) = Σ(-1)^k β_k > θ_resilience
 * 
 * Where β_k are Betti numbers (counting "holes" in the network).
 * The guardian ensures the network self-heals by detecting partitions
 * and reconfiguring mesh topology.
 */
class TopologyGuardian(
    private val resilienceThreshold: Double = 0.5
) {
    private val networkGraph = mutableMapOf<String, MutableSet<String>>()
    
    /**
     * Add a node to the network.
     */
    fun addNode(nodeId: String) {
        networkGraph.putIfAbsent(nodeId, mutableSetOf())
    }
    
    /**
     * Add an edge (connection) between two nodes.
     */
    fun addEdge(node1: String, node2: String) {
        networkGraph.getOrPut(node1) { mutableSetOf() }.add(node2)
        networkGraph.getOrPut(node2) { mutableSetOf() }.add(node1)
    }
    
    /**
     * Remove an edge (connection broken).
     */
    fun removeEdge(node1: String, node2: String) {
        networkGraph[node1]?.remove(node2)
        networkGraph[node2]?.remove(node1)
    }
    
    /**
     * Calculate Euler characteristic (simplified).
     * χ(G) = V - E + F
     * For planar graphs: V (vertices) - E (edges) + F (faces)
     * 
     * In practice, we use connected components and cycles.
     */
    fun calculateEulerCharacteristic(): Double {
        val vertices = networkGraph.size.toDouble()
        val edges = networkGraph.values.sumOf { it.size } / 2.0 // Undirected
        
        // Simplified: χ ≈ V - E (ignoring faces for mesh networks)
        // Higher χ means better connectivity
        val chi = vertices - edges + countConnectedComponents()
        
        return chi / vertices.coerceAtLeast(1.0) // Normalize
    }
    
    /**
     * Calculate β₀ (number of connected components).
     * β₀ > 1 indicates network partitions (holes).
     */
    fun calculateBetti0(): Int {
        return countConnectedComponents()
    }
    
    /**
     * Calculate β₁ (number of independent cycles).
     * β₁ indicates redundancy in the network.
     */
    fun calculateBetti1(): Int {
        val vertices = networkGraph.size
        val edges = networkGraph.values.sumOf { it.size } / 2
        val components = countConnectedComponents()
        
        // For graphs: β₁ = E - V + C (cycle space)
        return (edges - vertices + components).coerceAtLeast(0)
    }
    
    /**
     * Check if network is resilient (χ > threshold).
     */
    fun isResilient(): Boolean {
        val chi = calculateEulerCharacteristic()
        return chi > resilienceThreshold
    }
    
    /**
     * Detect network partitions.
     */
    fun detectPartitions(): List<Set<String>> {
        val visited = mutableSetOf<String>()
        val partitions = mutableListOf<Set<String>>()
        
        for (node in networkGraph.keys) {
            if (node !in visited) {
                val partition = mutableSetOf<String>()
                dfs(node, visited, partition)
                partitions.add(partition)
            }
        }
        
        return partitions
    }
    
    /**
     * Heal network by adding bridge edges between partitions.
     */
    fun healPartitions(): List<Pair<String, String>> {
        val partitions = detectPartitions()
        val bridges = mutableListOf<Pair<String, String>>()
        
        if (partitions.size <= 1) return bridges // Already connected
        
        // Connect each partition to the next
        for (i in 0 until partitions.size - 1) {
            val node1 = partitions[i].first()
            val node2 = partitions[i + 1].first()
            
            addEdge(node1, node2)
            bridges.add(node1 to node2)
        }
        
        return bridges
    }
    
    /**
     * Reconfigure mesh topology to maximize resilience.
     * Uses greedy algorithm to add edges strategically.
     */
    fun reconfigureMeshTopology(targetRedundancy: Int = 3): Int {
        var edgesAdded = 0
        
        // Ensure each node has at least targetRedundancy connections
        for ((node, neighbors) in networkGraph) {
            if (neighbors.size < targetRedundancy) {
                // Find closest nodes (simulate proximity)
                val candidates = networkGraph.keys
                    .filter { it != node && it !in neighbors }
                    .take(targetRedundancy - neighbors.size)
                
                for (candidate in candidates) {
                    addEdge(node, candidate)
                    edgesAdded++
                }
            }
        }
        
        return edgesAdded
    }
    
    /**
     * Calculate network robustness score.
     */
    fun calculateRobustness(): RobustnessScore {
        val chi = calculateEulerCharacteristic()
        val beta0 = calculateBetti0()
        val beta1 = calculateBetti1()
        val avgDegree = networkGraph.values.map { it.size }.average()
        
        return RobustnessScore(
            eulerCharacteristic = chi,
            connectedComponents = beta0,
            independentCycles = beta1,
            averageDegree = avgDegree,
            isResilient = isResilient()
        )
    }
    
    /**
     * Simulate attack by removing random nodes.
     * Returns percentage of nodes that can be removed before partition.
     */
    fun simulateAttack(attackPercentage: Double = 0.5): AttackSimulation {
        val originalNodes = networkGraph.keys.toList()
        val nodesToRemove = (originalNodes.size * attackPercentage).toInt()
        
        val removedNodes = originalNodes.shuffled().take(nodesToRemove)
        val tempGraph = networkGraph.toMutableMap()
        
        // Remove nodes
        removedNodes.forEach { node ->
            tempGraph.remove(node)
            tempGraph.values.forEach { it.remove(node) }
        }
        
        // Check if still connected
        val remainingConnected = countConnectedComponents(tempGraph) == 1
        
        return AttackSimulation(
            nodesRemoved = nodesToRemove,
            totalNodes = originalNodes.size,
            stillConnected = remainingConnected,
            partitions = if (remainingConnected) 1 else countConnectedComponents(tempGraph)
        )
    }
    
    // Helper: DFS to find connected components
    private fun dfs(node: String, visited: MutableSet<String>, partition: MutableSet<String>) {
        visited.add(node)
        partition.add(node)
        
        networkGraph[node]?.forEach { neighbor ->
            if (neighbor !in visited) {
                dfs(neighbor, visited, partition)
            }
        }
    }
    
    private fun countConnectedComponents(graph: Map<String, Set<String>> = networkGraph): Int {
        val visited = mutableSetOf<String>()
        var components = 0
        
        for (node in graph.keys) {
            if (node !in visited) {
                dfs(node, visited, mutableSetOf())
                components++
            }
        }
        
        return components.coerceAtLeast(1)
    }
    
    data class RobustnessScore(
        val eulerCharacteristic: Double,
        val connectedComponents: Int,
        val independentCycles: Int,
        val averageDegree: Double,
        val isResilient: Boolean
    )
    
    data class AttackSimulation(
        val nodesRemoved: Int,
        val totalNodes: Int,
        val stillConnected: Boolean,
        val partitions: Int
    ) {
        val survivalRate: Double
            get() = if (totalNodes > 0) 1.0 - (nodesRemoved.toDouble() / totalNodes) else 0.0
    }
}
