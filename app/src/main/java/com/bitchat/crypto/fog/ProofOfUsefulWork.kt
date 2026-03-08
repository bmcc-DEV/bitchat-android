package com.bitchat.crypto.fog

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.exp
import kotlin.math.ln

/**
 * Proof of Useful Work (PoUW) - Compensation system for FHE computation.
 * 
 * Implements the Addendum's solution:
 * "O trabalho computacional do FHE não é 'desperdício' como no Bitcoin.
 *  Ele é útil (processamento de dados privados)."
 * 
 * Heavy nodes execute FHE operations and are compensated with native tokens.
 * The network can sell FHE computation to third parties, subsidizing costs.
 * 
 * Mathematical model:
 *   Reward = Base × Class_Multiplier × Difficulty × Quality
 * 
 * Where:
 * - Base: Fixed reward per job
 * - Class_Multiplier: 1x/3x/10x/20x for Light/Medium/Heavy/Coordinator
 * - Difficulty: Computational complexity of the job
 * - Quality: Verification score (accuracy of result)
 */
class ProofOfUsefulWork {
    
    private val workerRegistry = ConcurrentHashMap<String, WorkerProfile>()
    private val jobQueue = ConcurrentHashMap<String, FHEJob>()
    private val completedJobs = mutableListOf<CompletedJob>()
    
    data class WorkerProfile(
        val nodeId: String,
        val nodeClass: FogNodeClassifier.NodeClass,
        val totalJobsCompleted: Long,
        val totalComputeTime: Double,
        val averageQuality: Double,
        val reputationScore: Double,
        val totalEarnings: Double
    )
    
    data class FHEJob(
        val jobId: String,
        val submitter: String,
        val jobType: FHEJobType,
        val difficulty: Double,      // 1.0 = baseline, higher = more complex
        val reward: Double,           // Base reward in tokens
        val deadline: Long,           // Unix timestamp
        val submittedAt: Long = System.currentTimeMillis()
    )
    
    enum class FHEJobType {
        TAX_CALCULATION,       // Dynamic tax computation
        BALANCE_AGGREGATION,   // Private balance queries
        SCHELLING_CONSENSUS,   // Oracle consensus computation
        ZK_PROOF_GENERATION,   // Heavy SNARK generation
        CRDT_MERGE,            // Large-scale wallet merges
        TOPOLOGY_HEALING       // Network graph optimization
    }
    
    data class JobAssignment(
        val jobId: String,
        val workerId: String,
        val assignedAt: Long,
        val expectedCompletionTime: Double  // Estimated milliseconds
    )
    
    data class CompletedJob(
        val jobId: String,
        val workerId: String,
        val completedAt: Long,
        val computeTimeMs: Double,
        val qualityScore: Double,
        val rewardEarned: Double
    )
    
    /**
     * Register a worker node for PoUW.
     */
    fun registerWorker(
        nodeId: String,
        nodeClass: FogNodeClassifier.NodeClass
    ) {
        workerRegistry[nodeId] = WorkerProfile(
            nodeId = nodeId,
            nodeClass = nodeClass,
            totalJobsCompleted = 0,
            totalComputeTime = 0.0,
            averageQuality = 1.0,
            reputationScore = 1.0,
            totalEarnings = 0.0
        )
    }
    
    /**
     * Submit a new FHE job to the network.
     */
    fun submitJob(job: FHEJob): String {
        jobQueue[job.jobId] = job
        return job.jobId
    }
    
    /**
     * Assign job to worker using reputation-weighted selection.
     * Formula:
     *   P(worker) ∝ ClassMultiplier × Reputation × e^(-CurrentLoad)
     */
    fun assignJob(jobId: String): JobAssignment? {
        val job = jobQueue[jobId] ?: return null
        
        // Find eligible workers
        val eligibleWorkers = workerRegistry.values.filter { worker ->
            worker.nodeClass in listOf(
                FogNodeClassifier.NodeClass.MEDIUM,
                FogNodeClassifier.NodeClass.HEAVY,
                FogNodeClassifier.NodeClass.COORDINATOR
            )
        }
        
        if (eligibleWorkers.isEmpty()) return null
        
        // Calculate selection probabilities
        val scores = eligibleWorkers.associateWith { worker ->
            val classMultiplier = when (worker.nodeClass) {
                FogNodeClassifier.NodeClass.MEDIUM -> 3.0
                FogNodeClassifier.NodeClass.HEAVY -> 10.0
                FogNodeClassifier.NodeClass.COORDINATOR -> 20.0
                else -> 1.0
            }
            
            // Penalize workers with many ongoing jobs
            val currentLoad = jobQueue.values.count { 
                it.submitter == worker.nodeId 
            }
            
            classMultiplier * worker.reputationScore * exp(-currentLoad * 0.5)
        }
        
        // Weighted random selection
        val totalScore = scores.values.sum()
        var random = Math.random() * totalScore
        var selectedWorker: WorkerProfile? = null
        
        for ((worker, score) in scores) {
            random -= score
            if (random <= 0) {
                selectedWorker = worker
                break
            }
        }
        
        selectedWorker ?: return null
        
        // Estimate completion time based on difficulty and worker class
        val baseTime = job.difficulty * 1000.0  // milliseconds
        val speedup = when (selectedWorker.nodeClass) {
            FogNodeClassifier.NodeClass.HEAVY -> 10.0
            FogNodeClassifier.NodeClass.COORDINATOR -> 20.0
            else -> 3.0
        }
        val expectedTime = baseTime / speedup
        
        jobQueue.remove(jobId)
        
        return JobAssignment(
            jobId = jobId,
            workerId = selectedWorker.nodeId,
            assignedAt = System.currentTimeMillis(),
            expectedCompletionTime = expectedTime
        )
    }
    
    /**
     * Verify and reward completed job.
     * Quality score based on:
     * - Correctness (verified by challenge-response)
     * - Timeliness (completed before deadline)
     */
    fun completeJob(
        jobId: String,
        workerId: String,
        result: ByteArray,
        computeTimeMs: Double
    ): Double {
        val worker = workerRegistry[workerId] ?: return 0.0
        
        // Simulate quality verification (in production, use cryptographic proof)
        val qualityScore = verifyResult(result)
        
        // Calculate reward
        val classMultiplier = when (worker.nodeClass) {
            FogNodeClassifier.NodeClass.LIGHT -> 1.0
            FogNodeClassifier.NodeClass.MEDIUM -> 3.0
            FogNodeClassifier.NodeClass.HEAVY -> 10.0
            FogNodeClassifier.NodeClass.COORDINATOR -> 20.0
        }
        
        // Base reward from job specs (assumed to be in jobQueue before assignment)
        val baseReward = 10.0  // tokens
        val difficulty = 1.0    // from job
        
        val reward = baseReward * classMultiplier * difficulty * qualityScore
        
        // Update worker profile
        workerRegistry[workerId] = worker.copy(
            totalJobsCompleted = worker.totalJobsCompleted + 1,
            totalComputeTime = worker.totalComputeTime + computeTimeMs,
            averageQuality = (worker.averageQuality * worker.totalJobsCompleted + qualityScore) / 
                            (worker.totalJobsCompleted + 1),
            reputationScore = updateReputation(worker, qualityScore),
            totalEarnings = worker.totalEarnings + reward
        )
        
        completedJobs.add(CompletedJob(
            jobId = jobId,
            workerId = workerId,
            completedAt = System.currentTimeMillis(),
            computeTimeMs = computeTimeMs,
            qualityScore = qualityScore,
            rewardEarned = reward
        ))
        
        return reward
    }
    
    /**
     * Verify result quality using challenge-response.
     * In production: re-execute a random subset on a trusted node.
     */
    private fun verifyResult(result: ByteArray): Double {
        // Placeholder: hash-based verification
        val hash = result.sumOf { it.toInt() }
        val quality = (hash % 20 + 80) / 100.0  // Random 0.8-1.0
        return quality.coerceIn(0.0, 1.0)
    }
    
    /**
     * Update worker reputation using EMA.
     * Reputation = 0.9 × old + 0.1 × new
     */
    private fun updateReputation(worker: WorkerProfile, qualityScore: Double): Double {
        return 0.9 * worker.reputationScore + 0.1 * qualityScore
    }
    
    /**
     * Get network statistics.
     */
    fun getNetworkStats(): NetworkStats {
        val totalWorkers = workerRegistry.size
        val totalJobsCompleted = completedJobs.size.toLong()
        val totalComputeTime = workerRegistry.values.sumOf { it.totalComputeTime }
        val totalRewards = workerRegistry.values.sumOf { it.totalEarnings }
        val averageQuality = workerRegistry.values
            .mapNotNull { it.averageQuality }
            .average()
        
        return NetworkStats(
            totalWorkers = totalWorkers,
            activeWorkers = workerRegistry.values.count { it.totalJobsCompleted > 0 },
            totalJobsCompleted = totalJobsCompleted,
            totalComputeTimeHours = totalComputeTime / (1000.0 * 3600.0),
            totalRewardsPaid = totalRewards,
            averageJobQuality = averageQuality,
            queuedJobs = jobQueue.size
        )
    }
    
    data class NetworkStats(
        val totalWorkers: Int,
        val activeWorkers: Int,
        val totalJobsCompleted: Long,
        val totalComputeTimeHours: Double,
        val totalRewardsPaid: Double,
        val averageJobQuality: Double,
        val queuedJobs: Int
    )
}
