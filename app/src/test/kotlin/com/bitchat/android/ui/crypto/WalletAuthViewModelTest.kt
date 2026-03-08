package com.bitchat.android.ui.crypto

import org.junit.Assert.assertTrue
import org.junit.Test

class WalletAuthViewModelTest {
    @Test
    fun `register and login updates status`() {
        val vm = WalletAuthViewModel()
        vm.register("1234")
        vm.login("1234")
        assertTrue(vm.status.value.startsWith("Authenticated:"))
    }
}
