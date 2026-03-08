package com.bitchat.crypto

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletAuthManagerTest {
    @Test
    fun `pin auth issues session`() {
        WalletAuthManager.registerPin("1234")
        assertTrue(WalletAuthManager.authenticate("1234"))
        assertNotNull(WalletAuthManager.currentSessionToken())
    }
}
