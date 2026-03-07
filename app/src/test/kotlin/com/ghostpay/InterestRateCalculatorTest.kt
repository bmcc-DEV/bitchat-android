package com.ghostpay

import com.ghostpay.android.pay.engine.*
import org.junit.Assert.*
import org.junit.Test

/**
 * JUnit tests for the i_p2p interest rate calculator.
 * Validates the three economic engines: Fisher, LiquidityCurve, SorosPremium.
 * No Android dependencies — pure Kotlin, runs on JVM.
 */
class InterestRateCalculatorTest {

    private val defaultConfig = EngineConfig(r0 = 0.02, alpha = 0.05, kappa = 1.5, lambda = 0.3)

    private fun state(
        v: Double = 10.0, t: Int = 100,
        dL: Double = 1.0, sL: Double = 1.0, sigma2: Double = 0.0
    ) = MarketState(v, t, dL, sL, sigma2)

    // ── Fisher Adjustment ───────────────────────────────────────────────────────

    @Test
    fun fisher_is_zero_when_velocity_unchanged() {
        val s = state(v = 10.0, t = 100)
        val p = state(v = 10.0, t = 90)
        val fisher = FisherAdjustment.compute(s, p)
        assertEquals(0.0, fisher, 0.001)
    }

    @Test
    fun fisher_is_positive_when_velocity_increases() {
        val s = state(v = 20.0, t = 110)
        val p = state(v = 10.0, t = 100)
        val fisher = FisherAdjustment.compute(s, p)
        assertTrue("Fisher must be positive with rising velocity", fisher > 0.0)
    }

    @Test
    fun fisher_is_clamped_to_max_half() {
        val s = state(v = 1000.0, t = 101)
        val p = state(v = 0.0, t = 100)
        val fisher = FisherAdjustment.compute(s, p)
        assertTrue("Fisher must be clamped at 0.5", fisher <= 0.5)
    }

    // ── Liquidity Curve ────────────────────────────────────────────────────────

    @Test
    fun liquidity_curve_at_balanced_demand_equals_near_r0() {
        val s = state(dL = 1.0, sL = 1.0)
        val rate = LiquidityCurve.compute(s, defaultConfig)
        // DL/SL = 1, so curve = R0 + alpha * 1^kappa = 0.02 + 0.05 = 0.07
        assertEquals(0.07, rate, 0.001)
    }

    @Test
    fun high_demand_spikes_liquidity_curve() {
        val s = state(dL = 10.0, sL = 1.0)
        val rate = LiquidityCurve.compute(s, defaultConfig)
        assertTrue("High demand must spike rate above 0.3", rate > 0.3)
    }

    @Test
    fun zero_supply_gives_max_ratio_capped_at_10() {
        val s = state(dL = 1.0, sL = 0.0)
        val rate = LiquidityCurve.compute(s, defaultConfig)
        // ratio = 10 (capped), curve = 0.02 + 0.05 * 10^1.5 ≈ 1.60
        assertTrue("Zero supply must generate very high rate", rate > 1.0)
    }

    // ── Soros Premium ─────────────────────────────────────────────────────────

    @Test
    fun soros_premium_is_zero_with_zero_volatility() {
        val s = state(sigma2 = 0.0)
        assertEquals(0.0, SorosPremium.compute(s, defaultConfig), 0.0001)
    }

    @Test
    fun soros_premium_scales_with_volatility() {
        val s = state(sigma2 = 2.0)
        val prem = SorosPremium.compute(s, defaultConfig)
        assertEquals(0.6, prem, 0.001)  // 0.3 * 2.0 = 0.6
    }

    // ── Combined i_p2p ────────────────────────────────────────────────────────

    @Test
    fun stable_market_yields_near_r0_plus_balanced_curve() {
        val s = state(v = 10.0, t = 100, dL = 1.0, sL = 1.0, sigma2 = 0.0)
        val p = state(v = 10.0, t = 90,  dL = 1.0, sL = 1.0, sigma2 = 0.0)
        val rate = InterestRateCalculator.calculate(s, p, defaultConfig)
        // Fisher=0, Curve≈0.07, Soros=0 → i_p2p ≈ 0.07
        assertTrue("Stable rate must be near 7%", rate in 0.06..0.10)
    }

    @Test
    fun high_demand_high_volatility_causes_high_rate() {
        val s = state(v = 30.0, t = 110, dL = 10.0, sL = 1.0, sigma2 = 3.0)
        val p = state(v = 10.0, t = 100, dL = 1.0,  sL = 1.0, sigma2 = 0.0)
        val rate = InterestRateCalculator.calculate(s, p, defaultConfig)
        assertTrue("High demand/volatility must exceed 1.0 (100% p.a.)", rate > 1.0)
    }

    @Test
    fun rate_is_always_floored_at_r0() {
        val s = state(v = 0.0, t = 100, dL = 0.0, sL = 10.0, sigma2 = 0.0)
        val p = state(v = 5.0, t = 110, dL = 0.0, sL = 10.0, sigma2 = 0.0)
        val rate = InterestRateCalculator.calculate(s, p, defaultConfig)
        assertTrue("Rate must never drop below R0 (2%)", rate >= defaultConfig.r0)
    }

    @Test
    fun rate_is_capped_at_500_percent() {
        val s = state(v = 9999.0, t = 10001, dL = 99999.0, sL = 0.001, sigma2 = 100.0)
        val p = state(v = 0.0,    t = 10000, dL = 0.0,     sL = 0.001, sigma2 = 0.0)
        val rate = InterestRateCalculator.calculate(s, p, defaultConfig)
        assertTrue("Rate must never exceed 500%", rate <= 5.0)
    }

    @Test
    fun format_rate_returns_correct_string() {
        val formatted = InterestRateCalculator.formatRate(0.085)
        assertEquals("8.5% a.a.", formatted)
    }
}
