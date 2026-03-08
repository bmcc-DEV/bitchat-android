package com.bitchat.crypto.bics

import org.junit.Assert.*
import org.junit.Test

class DynamicTaxCalculatorTest {
    
    @Test
    fun `optimal tax is below SWIFT cost`() {
        val calculator = DynamicTaxCalculator()
        
        val tax = calculator.calculateOptimalTax(
            swiftCost = 0.05,    // 5% banking fee
            inflation = 0.06,     // 6% annual inflation
            zkStrength = 0.8      // 80% anonymity
        )
        
        // Tax should be much lower than 5% + 0.5% monthly inflation
        assertTrue(tax < 0.055)
    }

    @Test
    fun `higher anonymity reduces tax`() {
        val calculator = DynamicTaxCalculator()
        
        val lowAnonymityTax = calculator.calculateOptimalTax(
            swiftCost = 0.05,
            inflation = 0.06,
            zkStrength = 0.2 // Low anonymity
        )
        
        val highAnonymityTax = calculator.calculateOptimalTax(
            swiftCost = 0.05,
            inflation = 0.06,
            zkStrength = 0.9 // High anonymity
        )
        
        assertTrue(highAnonymityTax < lowAnonymityTax)
    }

    @Test
    fun `transaction tax calculation`() {
        val calculator = DynamicTaxCalculator()
        
        val tax = calculator.calculateTransactionTax(
            amount = 1000.0,
            swiftCost = 0.03,
            inflation = 0.05,
            zkStrength = 0.7
        )
        
        // Should be small fraction of transaction
        assertTrue(tax > 0.0)
        assertTrue(tax < 50.0) // Less than 5%
    }

    @Test
    fun `deflation power increases with volume`() {
        val calculator = DynamicTaxCalculator()
        
        val lowVolume = calculator.calculateDeflationPower(
            taxRate = 0.02,
            transactionVolume = 1000.0,
            monetaryBase = 100000.0
        )
        
        val highVolume = calculator.calculateDeflationPower(
            taxRate = 0.02,
            transactionVolume = 10000.0,
            monetaryBase = 100000.0
        )
        
        assertTrue(highVolume > lowVolume)
    }

    @Test
    fun `volatility power converts panic to profit`() {
        val calculator = DynamicTaxCalculator()
        
        val power = calculator.calculateVolatilityPower(
            volatility = 0.3,        // 30% volatility
            liquidityDepth = 1000000.0,
            marketEntropy = 2.0      // High panic
        )
        
        assertTrue(power > 0.0)
    }

    @Test
    fun `P2P interest rate responds to liquidity`() {
        val calculator = DynamicTaxCalculator()
        
        val lowDemand = calculator.calculateP2PInterestRate(
            fisherAdjustment = 0.02,
            liquidityDemand = 100.0,
            liquiditySupply = 1000.0 // High supply
        )
        
        val highDemand = calculator.calculateP2PInterestRate(
            fisherAdjustment = 0.02,
            liquidityDemand = 1000.0,
            liquiditySupply = 100.0 // Low supply
        )
        
        assertTrue(highDemand > lowDemand)
    }

    @Test
    fun `P2P interest includes Soros volatility premium`() {
        val calculator = DynamicTaxCalculator()
        
        val lowVol = calculator.calculateP2PInterestRate(
            fisherAdjustment = 0.02,
            liquidityDemand = 500.0,
            liquiditySupply = 500.0,
            volatility = 0.05 // Low volatility
        )
        
        val highVol = calculator.calculateP2PInterestRate(
            fisherAdjustment = 0.02,
            liquidityDemand = 500.0,
            liquiditySupply = 500.0,
            volatility = 0.30 // High volatility
        )
        
        assertTrue(highVol > lowVol)
    }

    @Test
    fun `interest rate bounded within reasonable range`() {
        val calculator = DynamicTaxCalculator()
        
        val rate = calculator.calculateP2PInterestRate(
            fisherAdjustment = 0.05,
            liquidityDemand = 1000.0,
            liquiditySupply = 100.0,
            volatility = 0.5
        )
        
        // Should be between 1% and 50%
        assertTrue(rate >= 0.01)
        assertTrue(rate <= 0.50)
    }
}
