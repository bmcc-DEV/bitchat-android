package com.ghostpay.android.pay.token

/**
 * FNT (Fantasma Network Token) types + PAY_BIX utility token.
 *
 * FNT_B (Bearer)    — created by buyer, 72h TTL, only valid paired with FNT_C
 * FNT_C (Commodity) — created by seller, 168h TTL, always backed by physical unit
 * FNT_D (Governance)— minted by burning 1 FNT_B + 1 FNT_C; enables network voting
 * PAY_BIX            — platform utility token, halvable emission via PayBixRewardEngine
 */
enum class TokenType(val code: Byte) {
    FNT_B(0x01),
    FNT_C(0x02),
    FNT_D(0x03),
    PAY_BIX(0x04);

    companion object {
        fun fromCode(code: Byte): TokenType? = entries.firstOrNull { it.code == code }
    }
}
