package com.bitchat.crypto.oracle

import kotlin.math.abs

/**
 * Schelling Point Consensus for Edge Oracles.
 * Implements fault-tolerant data aggregation using game theory.
 * 
 * Mathematical foundation:
 *   V_oracle = argmin_v Σ w_i |v_i - v|
 * 
 * This finds the weighted median that minimizes total deviation,
 * making it resistant to outliers and malicious oracles.
 */
class SchellingConsensus {
    
    data class OracleReport(
        val oracleId: String,
        val value: Double,
        val weight: Double = 1.0,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Calculate weighted median using iterative minimization.
     * This is the Schelling Point - the natural focal point for coordination.
     */
    fun calculateWeightedMedian(reports: List<OracleReport>): Double {
        if (reports.isEmpty()) throw IllegalArgumentException("No reports to process")
        if (reports.size == 1) return reports[0].value

        // Sort reports by value
        val sorted = reports.sortedBy { it.value }

        // Calculate cumulative weights
        val totalWeight = sorted.sumOf { it.weight }
        val halfWeight = totalWeight / 2.0

        var cumulativeWeight = 0.0
        for (report in sorted) {
            cumulativeWeight += report.weight
            if (cumulativeWeight >= halfWeight) {
                return report.value
            }
        }

        // Fallback to simple median
        return sorted[sorted.size / 2].value
    }

    /**
     * Alternative: Minimize absolute deviation (more robust).
     * V_oracle = argmin_v Σ w_i |v_i - v|
     */
    fun calculateMinimumDeviation(reports: List<OracleReport>): Double {
        if (reports.isEmpty()) throw IllegalArgumentException("No reports to process")
        if (reports.size == 1) return reports[0].value

        // Grid search over candidate values (in production, use gradient descent)
        val candidates = reports.map { it.value }.sorted()
        var bestCandidate = candidates[0]
        var minDeviation = Double.MAX_VALUE

        for (candidate in candidates) {
            val deviation = reports.sumOf { report ->
                report.weight * abs(report.value - candidate)
            }
            
            if (deviation < minDeviation) {
                minDeviation = deviation
                bestCandidate = candidate
            }
        }

        return bestCandidate
    }

    /**
     * Update oracle reputations based on consensus accuracy.
     * Oracles that consistently report close to consensus gain reputation.
     */
    fun updateReputations(
        reports: List<OracleReport>,
        consensus: Double,
        sensors: Map<String, EdgeOracleSensor>
    ) {
        for (report in reports) {
            val deviation = abs(report.value - consensus)
            val maxDeviation = reports.maxOf { abs(it.value - consensus) }
            
            // Calculate accuracy: closer to consensus = higher accuracy
            val accuracy = if (maxDeviation > 0) {
                1.0 - (deviation / maxDeviation)
            } else {
                1.0
            }

            sensors[report.oracleId]?.updateReputation(accuracy)
        }
    }

    /**
     * Filter out obvious outliers (beyond 3 standard deviations).
     * This implements the "Veto Ancestral" for malicious oracles.
     */
    fun filterOutliers(reports: List<OracleReport>): List<OracleReport> {
        if (reports.size < 3) return reports

        val mean = reports.map { it.value }.average()
        val variance = reports.map { (it.value - mean) * (it.value - mean) }.average()
        val stdDev = kotlin.math.sqrt(variance)

        return reports.filter { report ->
            abs(report.value - mean) <= 3 * stdDev
        }
    }

    /**
     * Execute full consensus protocol with outlier filtering.
     */
    fun executeConsensus(
        reports: List<OracleReport>,
        sensors: Map<String, EdgeOracleSensor>
    ): ConsensusResult {
        val filtered = filterOutliers(reports)
        val consensus = calculateWeightedMedian(filtered)
        
        updateReputations(filtered, consensus, sensors)

        return ConsensusResult(
            value = consensus,
            participatingOracles = filtered.size,
            totalOracles = reports.size,
            confidence = calculateConfidence(filtered, consensus)
        )
    }

    /**
     * Calculate confidence level based on agreement.
     */
    private fun calculateConfidence(reports: List<OracleReport>, consensus: Double): Double {
        if (reports.isEmpty()) return 0.0

        val avgDeviation = reports.map { abs(it.value - consensus) }.average()
        val maxDeviation = reports.maxOf { abs(it.value - consensus) }

        return if (maxDeviation > 0) {
            1.0 - (avgDeviation / maxDeviation)
        } else {
            1.0
        }
    }

    data class ConsensusResult(
        val value: Double,
        val participatingOracles: Int,
        val totalOracles: Int,
        val confidence: Double
    )
}
