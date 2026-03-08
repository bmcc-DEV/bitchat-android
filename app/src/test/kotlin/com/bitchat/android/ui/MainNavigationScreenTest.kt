package com.bitchat.android.ui

import androidx.compose.ui.graphics.vector.ImageVector
import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigationScreenTest {
    @Test
    fun `navigation tab count includes crypto and oracle`() {
        val tabs = NavigationTab.values()
        assertEquals(5, tabs.size)
        assertEquals("Crypto", tabs[2].title)
        assertEquals("Network", tabs[3].title)
        assertEquals("Oracle", tabs[4].title)
        // icon type check for oracle
        val icon: ImageVector = tabs[4].icon
        assertEquals(true, icon != null)
    }
}
