package com.bitchat.crypto.fog

import kotlin.math.log2

/**
 * Hierarchical Fog Computing - Node Classification System.
 * 
 * Implements the Addendum's solution to FHE latency:
 * - Light Nodes: Mobile/Edge devices, only verify ZK proofs
 * - Heavy Nodes: Dedicated GPU/TPU, execute full FHE computations
 * 
 * Classification based on:
 * - Computational power (benchmarked FLOPS)
 * - Battery status (mains-powered vs battery)
 * - Network position (uptime, bandwidth)
 * - Stake/reputation (Proof of Useful Work contribution)
 */
class FogNodeClassifier {
    
    data class NodeCapabilities(
        val deviceId: String,
        val computeScore: Double,    // Benchmarked FLOPS (0-1 normalized)
        val isPowered: Boolean,       // Mains-powered = true, battery = false
        val uptimeHours: Double,      // Historical uptime
        val bandwidthMbps: Double,    // Available bandwidth
        val reputationScore: Double   // Proof of Useful Work history
    )
    
    enum class NodeClass {
        LIGHT,      // Mobile/IoT - ZK verification only
        MEDIUM,     // Workstation - partial FHE operations
        HEAVY,      // GPU/TPU cluster - full FHE mining
        COORDINATOR // Regional coordinator - aggregates results
    }
    
    data class ClassificationResult(
        val nodeClass: NodeClass,
        val confidenceScore: Double,
        val recommendedWorkload: WorkloadType,
        val compensationMultiplier: Double  // PoUW reward multiplier
    )
    
    enum class WorkloadType {
        ZK_VERIFICATION,        // Light: verify SNARKs
        STATE_CHANNEL_ROUTING,  // Light: route off-chain channels
        FHE_PARTIAL,            // Medium: partial homomorphic ops
        FHE_FULL,               // Heavy: full FHE computation
        CONSENSUS_COORDINATION, // Coordinator: Schelling consensus
        DATA_MULE_BUFFER       // All: DTN store-and-forward
    }
    
    /**
     * Classify node using multi-criteria decision analysis.
     * Formula:
     *   Score = w_compute·C + w_power·P + w_uptime·U + w_bandwidth·B + w_reputation·R
     * 
     * Thresholds:
     * - Light: Score < 0.3
     * - Medium: 0.3 ≤ Score < 0.6
     * - Heavy: 0.6 ≤ Score < 0.9
     * - Coordinator: Score ≥ 0.9
     */
    fun classify(capabilities: NodeCapabilities): ClassificationResult {
        val weights = Weights(
            compute = 0.35,
            power = 0.20,
            uptime = 0.20,
            bandwidth = 0.15,
            reputation = 0.10
        )
        
        val powerScore = if (capabilities.isPowered) 1.0 else 0.2
        val uptimeScore = (capabilities.uptimeHours / 720.0).coerceIn(0.0, 1.0) // 720h = 1 month
        val bandwidthScore = (log2(capabilities.bandwidthMbps + 1) / 10.0).coerceIn(0.0, 1.0)
        
        val totalScore = 
            weights.compute * capabilities.computeScore +
            weights.power * powerScore +
            weights.uptime * uptimeScore +
            weights.bandwidth * bandwidthScore +
            weights.reputation * capabilities.reputationScore
        
        val nodeClass = when {
            totalScore >= 0.9 -> NodeClass.COORDINATOR
            totalScore >= 0.6 -> NodeClass.HEAVY
            totalScore >= 0.3 -> NodeClass.MEDIUM
            else -> NodeClass.LIGHT
        }
        
        val workload = when (nodeClass) {
            NodeClass.LIGHT -> if (capabilities.bandwidthScore > 0.5) 
                WorkloadType.STATE_CHANNEL_ROUTING 
            else 
                WorkloadType.ZK_VERIFICATION
            NodeClass.MEDIUM -> WorkloadType.FHE_PARTIAL
            NodeClass.HEAVY -> WorkloadType.FHE_FULL
            NodeClass.COORDINATOR -> WorkloadType.CONSENSUS_COORDINATION
        }
        
        // PoUW compensation: Heavy nodes earn 10x, Coordinators 20x
        val compensationMultiplier = when (nodeClass) {
            NodeClass.LIGHT -> 1.0
            NodeClass.MEDIUM -> 3.0
            NodeClass.HEAVY -> 10.0
            NodeClass.COORDINATOR -> 20.0
        }
        
        return ClassificationResult(
            nodeClass = nodeClass,
            confidenceScore = calculateConfidence(capabilities, totalScore),
            recommendedWorkload = workload,
            compensationMultiplier = compensationMultiplier
        )
    }
    
    /**
     * Benchmark device to determine compute score.
     * Runs a short FHE operation and measures time.
     */
    suspend fun benchmarkDevice(): Double {
        val startTime = System.nanoTime()
        
        // Simulate lightweight FHE-like operation (matrix multiplication)
        val matrixSize = 64
        val a = Array(matrixSize) { DoubleArray(matrixSize) { Math.random() } }
        val b = Array(matrixSize) { DoubleArray(matrixSize) { Math.random() } }
        val c = Array(matrixSize) { DoubleArray(matrixSize) }
        
        for (i in 0 until matrixSize) {
            for (j in 0 until matrixSize) {
                var sum = 0.0
                for (k in 0 until matrixSize) {
                    sum += a[i][k] * b[k][j]
                }
                c[i][j] = sum
            }
        }
        
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0
        
        // Normalize: 1000ms = 0.0, 10ms = 1.0 (log scale)
        val score = 1.0 - (log2(elapsedMs + 1) / 10.0).coerceIn(0.0, 1.0)
        
        return score
    }
    
    private fun calculateConfidence(capabilities: NodeCapabilities, score: Double): Double {
        // Confidence decreases if node is near class boundary
        val thresholds = listOf(0.3, 0.6, 0.9)
        val minDistance = thresholds.map { kotlin.math.abs(score - it) }.minOrNull() ?: 1.0
        
        return (minDistance * 2).coerceIn(0.5, 1.0)
    }
    
    private data class Weights(
        val compute: Double,
        val power: Double,
        val uptime: Double,
        val bandwidth: Double,
        val reputation: Double
    )
    
    private val NodeCapabilities.bandwidthScore: Double
        get() = (log2(bandwidthMbps + 1) / 10.0).coerceIn(0.0, 1.0)
}
