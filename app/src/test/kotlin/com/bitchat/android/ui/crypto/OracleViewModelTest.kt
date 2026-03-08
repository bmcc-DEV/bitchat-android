package com.bitchat.android.ui.crypto

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OracleViewModelTest {
    @Test
    fun `query returns nonempty result`() = runTest {
        val vm = OracleViewModel()
        vm.query("eth")
        val res = vm.price.value
        assertTrue(res.contains("ETH:"))
    }
}
