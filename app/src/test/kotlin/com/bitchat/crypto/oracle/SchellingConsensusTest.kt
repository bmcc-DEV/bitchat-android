package com.bitchat.crypto.oracle

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SchellingConsensusTest {
    
    @Test
    fun `weighted median with single report returns value`() {
        val consensus = SchellingConsensus()
        val reports = listOf(
            SchellingConsensus.OracleReport("oracle1", 100.0, 1.0)
        )
        
        val result = consensus.calculateWeightedMedian(reports)
        
        assertEquals(100.0, result, 1e-6)
    }

    @Test
    fun `weighted median finds middle value`() {
        val consensus = SchellingConsensus()
        val reports = listOf(
            SchellingConsensus.OracleReport("oracle1", 90.0, 1.0),
            SchellingConsensus.OracleReport("oracle2", 100.0, 1.0),
            SchellingConsensus.OracleReport("oracle3", 110.0, 1.0)
        )
        
        val result = consensus.calculateWeightedMedian(reports)
        
        assertEquals(100.0, result, 1e-6)
    }

    @Test
    fun `outlier filtering removes extremes`() {
        val consensus = SchellingConsensus()
        val reports = listOf(
            SchellingConsensus.OracleReport("oracle1", 100.0, 1.0),
            SchellingConsensus.OracleReport("oracle2", 105.0, 1.0),
            SchellingConsensus.OracleReport("oracle3", 102.0, 1.0),
            SchellingConsensus.OracleReport("malicious", 1000.0, 1.0) // Outlier
        )
        
        val filtered = consensus.filterOutliers(reports)
        
        assertEquals(3, filtered.size)
        assertFalse(filtered.any { it.oracleId == "malicious" })
    }

    @Test
    fun `reputation updates based on accuracy`() = runTest {
        val consensus = SchellingConsensus()
        val sensor1 = EdgeOracleSensor("oracle1")
        val sensor2 = EdgeOracleSensor("oracle2")
        val sensors = mapOf("oracle1" to sensor1, "oracle2" to sensor2)
        
        val reports = listOf(
            SchellingConsensus.OracleReport("oracle1", 100.0, 1.0),
            SchellingConsensus.OracleReport("oracle2", 150.0, 1.0)
        )
        
        val consensusValue = consensus.calculateWeightedMedian(reports)
        consensus.updateReputations(reports, consensusValue, sensors)
        
        // Oracle closer to consensus should have higher reputation
        assertTrue(sensor1.reputationScore > sensor2.reputationScore)
    }

    @Test
    fun `full consensus execution filters and updates`() = runTest {
        val consensus = SchellingConsensus()
        val sensors = (1..5).associate {
            "oracle$it" to EdgeOracleSensor("oracle$it")
        }
        
        val reports = listOf(
            SchellingConsensus.OracleReport("oracle1", 100.0, 1.0),
            SchellingConsensus.OracleReport("oracle2", 102.0, 1.0),
            SchellingConsensus.OracleReport("oracle3", 98.0, 1.0),
            SchellingConsensus.OracleReport("oracle4", 101.0, 1.0),
            SchellingConsensus.OracleReport("oracle5", 500.0, 1.0) // Outlier
        )
        
        val result = consensus.executeConsensus(reports, sensors)
        
        assertTrue(result.value in 98.0..102.0)
        assertEquals(4, result.participatingOracles) // Outlier filtered
        assertTrue(result.confidence > 0.5)
    }

    @Test
    fun `minimum deviation calculation`() {
        val consensus = SchellingConsensus()
        val reports = listOf(
            SchellingConsensus.OracleReport("oracle1", 95.0, 1.0),
            SchellingConsensus.OracleReport("oracle2", 100.0, 2.0), // Higher weight
            SchellingConsensus.OracleReport("oracle3", 105.0, 1.0)
        )
        
        val result = consensus.calculateMinimumDeviation(reports)
        
        // Should favor higher-weighted report
        assertEquals(100.0, result, 5.0) // Within 5 units
    }
}
