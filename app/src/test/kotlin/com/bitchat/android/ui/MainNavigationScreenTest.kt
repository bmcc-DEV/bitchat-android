package com.bitchat.android.ui

import androidx.compose.ui.graphics.vector.ImageVector
import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigationScreenTest {
    @Test
    fun `navigation tab count includes crypto`() {
        val tabs = NavigationTab.values()
        assertEquals(3, tabs.size)
        assertEquals("Crypto", tabs[2].title)
        // icon type check
        val icon: ImageVector = tabs[2].icon
        assertEquals(true, icon != null)
    }
}
