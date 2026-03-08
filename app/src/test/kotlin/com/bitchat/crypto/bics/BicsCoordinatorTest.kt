package com.bitchat.crypto.bics

import com.bitchat.crypto.CryptoLedger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BicsCoordinatorTest {
    
    private lateinit var coordinator: BicsCoordinator
    private lateinit var ledger: CryptoLedger
    
    @Before
    fun setup() {
        ledger = CryptoLedger(taxRate = 0.05)
        coordinator = BicsCoordinator(ledger)
    }
    
    @After
    fun teardown() {
        coordinator.shutdown()
    }
    
    @Test
    fun `transaction processes through all BICS layers`() = runTest {
        val result = coordinator.processTransaction(
            from = "alice",
            to = "bob",
            amount = 1000.0,
            zkStrength = 0.8
        )
        
        assertTrue(result.success)
        assertTrue(result.taxCharged > 0.0)
        assertTrue(result.burned > 0.0)
        assertTrue(result.deflationRate >= 0.0)
    }
    
    @Test
    fun `dynamic tax is lower than SWIFT cost`() = runTest {
        val result = coordinator.processTransaction(
            from = "alice",
            to = "bob",
            amount = 1000.0,
            zkStrength = 0.7
        )
        
        // Tax rate should be much lower than typical 3-5% SWIFT fees
        assertTrue(result.taxRate < 0.05)
    }
    
    @Test
    fun `higher anonymity reduces tax rate`() = runTest {
        val lowAnon = coordinator.processTransaction(
            from = "alice",
            to = "bob",
            amount = 1000.0,
            zkStrength = 0.2
        )
        
        val highAnon = coordinator.processTransaction(
            from = "alice",
            to = "bob",
            amount = 1000.0,
            zkStrength = 0.9
        )
        
        assertTrue(highAnon.taxRate < lowAnon.taxRate)
    }
    
    @Test
    fun `system state tracks all layers`() = runTest {
        coordinator.processTransaction("alice", "bob", 1000.0)
        coordinator.processTransaction("bob", "charlie", 500.0)
        
        val state = coordinator.getSystemState()
        
        assertTrue(state.totalBurned > 0.0)
        assertTrue(state.totalLocked > 0.0)
        assertTrue(state.portfolioRisk >= 0.0)
    }
    
    @Test
    fun `loan rate increases with demand`() {
        val lowDemandRate = coordinator.calculateLoanRate(
            collateralAsset = "BTC",
            collateralVolatility = 0.2,
            requestedAmount = 100.0
        )
        
        val highDemandRate = coordinator.calculateLoanRate(
            collateralAsset = "BTC",
            collateralVolatility = 0.2,
            requestedAmount = 10000.0
        )
        
        assertTrue(highDemandRate > lowDemandRate)
    }
    
    @Test
    fun `loan rate increases with volatility`() {
        val lowVolRate = coordinator.calculateLoanRate(
            collateralAsset = "USDC",
            collateralVolatility = 0.01, // Stablecoin
            requestedAmount = 1000.0
        )
        
        val highVolRate = coordinator.calculateLoanRate(
            collateralAsset = "SHIB",
            collateralVolatility = 0.5, // Memecoin
            requestedAmount = 1000.0
        )
        
        assertTrue(highVolRate > lowVolRate)
    }
    
    @Test
    fun `tax allocation splits correctly`() = runTest {
        val result = coordinator.processTransaction(
            from = "alice",
            to = "bob",
            amount = 1000.0
        )
        
        val state = coordinator.getSystemState()
        
        // 50% burned, 30% locked, 15% arb, 5% innovation
        // Burned should be roughly half of tax
        val expectedBurn = result.taxCharged * 0.5
        assertEquals(expectedBurn, result.burned, result.taxCharged * 0.1)
    }
    
    @Test
    fun `system energy increases with activity`() = runTest {
        val result1 = coordinator.processTransaction("alice", "bob", 1000.0)
        val energy1 = result1.systemEnergy
        
        coordinator.processTransaction("bob", "charlie", 500.0)
        val result2 = coordinator.processTransaction("charlie", "alice", 250.0)
        val energy2 = result2.systemEnergy
        
        // Energy should increase as liquidity pools grow
        assertTrue(energy2 > energy1)
    }
}
