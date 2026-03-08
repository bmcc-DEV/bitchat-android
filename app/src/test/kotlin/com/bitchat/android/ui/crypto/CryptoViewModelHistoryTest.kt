package com.bitchat.android.ui.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CryptoViewModelHistoryTest {
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
    fun `history updates after transfer`() = runTest(testDispatcher) {
        val vm = CryptoViewModel()
        vm.transfer("alice", "bob", 50.0)
        testScheduler.advanceUntilIdle()
        val hist = vm.history.value
        assertTrue(hist.isNotEmpty())
    }
}
