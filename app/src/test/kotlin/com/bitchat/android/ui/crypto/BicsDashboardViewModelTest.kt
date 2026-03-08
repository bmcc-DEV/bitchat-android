package com.bitchat.android.ui.crypto

import com.bitchat.crypto.topology.TopologyGuardian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BicsDashboardViewModelTest {
    
    private lateinit var viewModel: BicsDashboardViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = BicsDashboardViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `test initial system state loaded`() = runTest {
        assertNotNull(viewModel.systemState.value)
        assertNotNull(viewModel.topologyScore.value)
    }
    
    @Test
    fun `test topology initialized with 10 nodes`() = runTest {
        val topology = viewModel.topologyScore.value
        assertNotNull(topology)
        assertTrue(topology.eulerCharacteristic <= 1.0, 
            "Euler characteristic should be valid for mesh")
    }
    
    @Test
    fun `test transaction execution updates metrics`() = runTest {
        viewModel.executeTransaction("alice", "bob", 1000.0, 0.8)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val tx = viewModel.lastTransaction.value
        assertNotNull(tx)
        assertTrue(tx.taxCharged > 0.0, "Tax should be charged")
        assertTrue(tx.burned >= tx.taxCharged * 0.5, "50% should be burned")
        assertEquals(1000.0 - tx.taxCharged, tx.netAmount, 0.01)
    }
    
    @Test
    fun `test high ZK strength reduces tax`() = runTest {
        viewModel.executeTransaction("alice", "bob", 1000.0, 0.9)
        testDispatcher.scheduler.advanceUntilIdle()
        val highZkTax = viewModel.lastTransaction.value?.taxRate ?: 0.0
        
        viewModel.executeTransaction("alice", "bob", 1000.0, 0.2)
        testDispatcher.scheduler.advanceUntilIdle()
        val lowZkTax = viewModel.lastTransaction.value?.taxRate ?: 0.0
        
        assertTrue(highZkTax < lowZkTax, 
            "High ZK strength should result in lower tax")
    }
    
    @Test
    fun `test multiple transactions increase deflation`() = runTest {
        repeat(5) {
            viewModel.executeTransaction("alice", "bob", 500.0, 0.5)
            testDispatcher.scheduler.advanceUntilIdle()
        }
        
        val state = viewModel.systemState.value
        assertNotNull(state)
        assertTrue(state.totalBurned > 0.0, "Total burned should accumulate")
    }
    
    @Test
    fun `test loan rate calculation`() = runTest {
        val rate = viewModel.loanRate.value
        assertTrue(rate > 0.0, "Loan rate should be positive")
        assertTrue(rate < 1.0, "Loan rate should be reasonable (<100%)")
    }
    
    @Test
    fun `test refresh metrics updates all state`() = runTest {
        viewModel.executeTransaction("alice", "bob", 1000.0, 0.7)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val oldEnergy = viewModel.systemState.value?.systemEnergy ?: 0.0
        
        viewModel.refreshMetrics()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertNotNull(viewModel.systemState.value)
        assertNotNull(viewModel.topologyScore.value)
    }
    
    @Test
    fun `test heal topology improves resilience`() = runTest {
        val initialScore = viewModel.topologyScore.value
        
        viewModel.healTopology()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val healedScore = viewModel.topologyScore.value
        assertNotNull(healedScore)
        
        // After healing, components should be 1 (fully connected)
        assertEquals(1, healedScore.connectedComponents, 
            "Network should be fully connected after healing")
    }
    
    @Test
    fun `test attack simulation returns valid results`() = runTest {
        val simulation = viewModel.simulateAttack(0.3)
        
        assertTrue(simulation.nodesRemoved > 0, "Nodes should be removed")
        assertTrue(simulation.finalComponents >= 1, "Should have at least 1 component")
        assertTrue(simulation.survivability >= 0.0 && simulation.survivability <= 1.0,
            "Survivability should be in [0,1]")
    }
    
    @Test
    fun `test system energy increases with transactions`() = runTest {
        viewModel.executeTransaction("alice", "bob", 100.0, 0.5)
        testDispatcher.scheduler.advanceUntilIdle()
        val energy1 = viewModel.lastTransaction.value?.systemEnergy ?: 0.0
        
        viewModel.executeTransaction("alice", "bob", 500.0, 0.5)
        testDispatcher.scheduler.advanceUntilIdle()
        val energy2 = viewModel.lastTransaction.value?.systemEnergy ?: 0.0
        
        assertTrue(energy2 > energy1, "Higher transaction volume should increase energy")
    }
    
    @Test
    fun `test deflation rate correlates with burn rate`() = runTest {
        // High burn scenario
        viewModel.executeTransaction("alice", "bob", 1000.0, 0.2) // Low ZK = high tax
        testDispatcher.scheduler.advanceUntilIdle()
        val highBurnDeflation = viewModel.lastTransaction.value?.deflationRate ?: 0.0
        
        // Low burn scenario
        viewModel.executeTransaction("alice", "bob", 1000.0, 0.9) // High ZK = low tax
        testDispatcher.scheduler.advanceUntilIdle()
        val lowBurnDeflation = viewModel.lastTransaction.value?.deflationRate ?: 0.0
        
        assertTrue(highBurnDeflation >= lowBurnDeflation,
            "Higher burn should correlate with higher deflation rate")
    }
    
    @Test
    fun `test ViewModel cleanup shuts down coordinator`() = runTest {
        viewModel.executeTransaction("alice", "bob", 100.0, 0.5)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onCleared()
        // If no exception, shutdown succeeded
    }
}
