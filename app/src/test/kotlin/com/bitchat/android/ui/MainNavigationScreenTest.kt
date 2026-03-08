package com.bitchat.android.ui

import androidx.compose.ui.graphics.vector.ImageVector
import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigationScreenTest {
    @Test
    fun `navigation tab count includes crypto oracle and wallet`() {
        val tabs = NavigationTab.values()
        assertEquals(6, tabs.size)
        assertEquals("Crypto", tabs[2].title)
        assertEquals("Network", tabs[3].title)
        assertEquals("Oracle", tabs[4].title)
        assertEquals("Wallet", tabs[5].title)
        // icon type check for wallet
        val icon: ImageVector = tabs[5].icon
        assertEquals(true, icon != null)
    }
}
