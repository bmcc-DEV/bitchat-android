package com.bitchat.crypto.bics

import org.junit.Assert.*
import org.junit.Test

class BicsLayersTest {
    
    @Test
    fun `survival layer burns fiat base`() {
        val survival = BicsLayers.SurvivalLayer()
        
        survival.burnFiatBase(100.0, "tax")
        
        assertEquals(100.0, survival.getTotalBurned(), 1e-6)
    }

    @Test
    fun `survival layer calculates deflation`() {
        val survival = BicsLayers.SurvivalLayer()
        survival.burnFiatBase(50.0)
        
        val deflation = survival.calculateDeflation(
            monetaryBase = 1000.0,
            velocity = 2.0,
            transactions = 100.0
        )
        
        // 50/1000 = 5% deflation
        assertEquals(0.05, deflation, 0.01)
    }

    @Test
    fun `stability layer locks collateral`() {
        val stability = BicsLayers.StabilityLayer()
        
        val locked = stability.lockCollateral("alice", 1000.0)
        
        assertTrue(locked)
        assertEquals(1000.0, stability.getLockedAmount("alice"), 1e-6)
    }

    @Test
    fun `stability layer calculates yield`() {
        val stability = BicsLayers.StabilityLayer()
        stability.lockCollateral("alice", 1000.0)
        
        val yield = stability.calculateYield("alice", 365) // 1 year
        
        // 1000 * 0.08 * 1 = 80
        assertEquals(80.0, yield, 1.0)
    }

    @Test
    fun `distortion layer detects asymmetry`() {
        val distortion = BicsLayers.DistortionLayer()
        
        val alpha = distortion.detectAsymmetry(
            fiatPrice = 100.0,
            meshPrice = 90.0,
            chaos = 1.0
        )
        
        // (100 - 90)² * 1.0 = 100
        assertEquals(100.0, alpha, 1e-6)
    }

    @Test
    fun `distortion layer executes arbitrage on spread`() {
        val distortion = BicsLayers.DistortionLayer()
        
        val profit = distortion.executeArbitrage(
            asset = "BTC",
            fiatPrice = 100.0,
            meshPrice = 95.0,
            volume = 1000.0,
            threshold = 0.02
        )
        
        // Spread: 5%, Volume: 1000 → Profit: 50
        assertEquals(50.0, profit, 5.0)
    }

    @Test
    fun `arbitrage not executed below threshold`() {
        val distortion = BicsLayers.DistortionLayer()
        
        val profit = distortion.executeArbitrage(
            asset = "BTC",
            fiatPrice = 100.0,
            meshPrice = 99.0,
            volume = 1000.0,
            threshold = 0.02 // 2% minimum
        )
        
        assertEquals(0.0, profit, 1e-6)
    }

    @Test
    fun `convexity layer funds innovation`() {
        val convexity = BicsLayers.ConvexityLayer()
        
        val funded = convexity.fundInnovation("ProjectX", 1000.0, 0.5)
        
        assertTrue(funded)
        assertEquals(1, convexity.getPortfolio().size)
    }

    @Test
    fun `convexity layer assesses portfolio risk`() {
        val convexity = BicsLayers.ConvexityLayer()
        convexity.fundInnovation("ProjectA", 1000.0, 0.3)
        convexity.fundInnovation("ProjectB", 1000.0, 0.7)
        
        val risk = convexity.assessRisk()
        
        // Weighted average: (0.5 * 0.3) + (0.5 * 0.7) = 0.5
        assertEquals(0.5, risk, 0.1)
    }
}

class BicsEnergyEngineTest {
    
    @Test
    fun `energy calculation includes kinetic and potential`() {
        val engine = BicsEnergyEngine()
        
        val energy = engine.computeEnergy(
            mass = 1000.0,
            velocity = 10.0,
            zkTax = 0.05
        )
        
        // ½ * 1000 * 100 + 0.05 * ln(1001)
        assertTrue(energy > 50000.0)
    }

    @Test
    fun `thermodynamics maintained with no entropy loss`() {
        val engine = BicsEnergyEngine()
        
        val entropy = engine.maintainThermodynamics(
            inflow = 1000.0,
            outflow = 950.0,
            friction = 50.0
        )
        
        // (1000 - 950) - 50 = 0 (perfect conservation)
        assertEquals(0.0, entropy, 1e-6)
    }

    @Test
    fun `thermodynamics detects entropy leak`() {
        val engine = BicsEnergyEngine()
        
        val entropy = engine.maintainThermodynamics(
            inflow = 1000.0,
            outflow = 900.0,
            friction = 50.0
        )
        
        // (1000 - 900) - 50 = 50 (leak detected)
        assertEquals(50.0, entropy, 1e-6)
    }
}
