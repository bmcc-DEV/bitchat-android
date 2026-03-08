package com.bitchat.android.ui.crypto

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CryptoViewModelHistoryTest {
    @Test
    fun `history updates after transfer`() = runTest {
        val vm = CryptoViewModel()
        vm.transfer("alice", "bob", 50.0)
        val hist = vm.history.value
        assertTrue(hist.isNotEmpty())
    }
}
