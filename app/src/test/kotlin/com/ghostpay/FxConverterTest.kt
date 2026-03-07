package com.ghostpay

import com.ghostpay.android.pay.fx.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FxConverter and SettlementRouter.
 * No Android dependencies — runs on JVM.
 */
class FxConverterTest {

    private val quote = FxQuote(
        fntToBrlRate  = 5.0,    // 1 FNT = R$ 5.00
        fntToBtcRate  = 0.000025, // 1 FNT = 2500 satoshis
        fntToUsdtRate = 1.0     // 1 FNT = 1.00 USDT
    )

    // ── FNT_INTERNAL (no conversion) ───────────────────────────────────────────

    @Test
    fun internal_settlement_returns_fnt_amount() {
        val result = FxConverter.convert(100.0, SettlementCurrency.FNT_INTERNAL, quote)
        assertTrue("FNT_INTERNAL must contain FNT", result.contains("FNT"))
        assertFalse("FNT_INTERNAL must not mention R$", result.contains("R\$"))
    }

    // ── BRL hint ──────────────────────────────────────────────────────────────

    @Test
    fun brl_conversion_multiplies_fnt_by_rate() {
        val result = FxConverter.convert(100.0, SettlementCurrency.BRL_HINT, quote)
        assertTrue("BRL hint must contain R\$", result.contains("R\$"))
        assertTrue("BRL hint must show 500.00", result.contains("500,00").or(result.contains("500.00")))
    }

    @Test
    fun brl_conversion_with_zero_rate_throws() {
        val zeroQuote = quote.copy(fntToBrlRate = 0.0)
        try {
            FxConverter.convert(100.0, SettlementCurrency.BRL_HINT, zeroQuote)
            fail("Should throw when BRL rate is 0")
        } catch (e: IllegalArgumentException) {
            assertTrue("Error must mention rate", e.message?.contains("rate") == true)
        }
    }

    // ── BTC hint ──────────────────────────────────────────────────────────────

    @Test
    fun btc_conversion_returns_satoshis() {
        val result = FxConverter.convert(100.0, SettlementCurrency.BTC_HINT, quote)
        assertTrue("BTC hint must contain 'sat'", result.contains("sat"))
        assertTrue("100 FNT * 0.000025 = 0.0025 BTC = 2500 sat", result.contains("2500"))
    }

    // ── USDT hint ─────────────────────────────────────────────────────────────

    @Test
    fun usdt_conversion_matches_rate() {
        val result = FxConverter.convert(50.0, SettlementCurrency.USDT_HINT, quote)
        assertTrue("USDT hint must contain USDT", result.contains("USDT"))
        assertTrue("50 FNT * 1.0 = 50.00 USDT", result.contains("50.00"))
    }

    // ── SettlementRouter ──────────────────────────────────────────────────────

    @Test
    fun settlement_router_returns_null_for_fnt_internal() {
        val router = SettlementRouter()
        router.setSellerPreference(SettlementCurrency.FNT_INTERNAL)
        router.updateQuote(quote)
        assertNull("No hint needed for internal settlement", router.settlementHint(100.0))
    }

    @Test
    fun settlement_router_returns_hint_for_brl() {
        val router = SettlementRouter()
        router.setSellerPreference(SettlementCurrency.BRL_HINT)
        router.updateQuote(quote)
        val hint = router.settlementHint(10.0)
        assertNotNull("Must return a hint for BRL preference", hint)
        assertTrue("Hint must reference R\$", hint!!.contains("R\$"))
    }
}
