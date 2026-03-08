package com.bitchat.android.ui.crypto

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkViewModelTest {
    @Test
    fun `sending message adds to history`() = runTest {
        val vm = NetworkViewModel()
        vm.sendMessage("hello")
        val msgs = vm.messages.value
        assertTrue(msgs.contains("hello"))
    }
}
