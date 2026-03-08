package com.bitchat.android.ui.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CryptoViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    @Test
    fun `initial balances populated`() = runTest {
        val vm = CryptoViewModel()
        val balances = vm.balances.value
        assertEquals(1000.0, balances["alice"]!!, 1e-6)
        assertEquals(500.0, balances["bob"]!!, 1e-6)
    }

    @Test
    fun `transfer updates balances`() = runTest(testDispatcher) {
        val vm = CryptoViewModel()
        vm.transfer("alice", "bob", 100.0)
        testScheduler.advanceUntilIdle()
        val balances = vm.balances.value
        assertEquals(900.0, balances["alice"]!!, 1e-6)
        // bob receives 95 after 5% tax
        assertEquals(595.0, balances["bob"]!!, 1e-6)
    }
}
