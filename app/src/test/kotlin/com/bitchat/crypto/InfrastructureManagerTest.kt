package com.bitchat.crypto

import org.junit.Assert.assertNotNull
import org.junit.Test

class FakeKernel : SecretKernel {
    var initialized = false
    override fun initialize() {
        initialized = true
    }

    override fun allocateEnclave(sizeBytes: Long) {
        // no-op
    }
}

class FakeSubOS : SubOS {
    override fun executeInstruction(instruction: ByteArray) {}
    override fun compileNetworkCode(bytecode: ByteArray): ByteArray {
        // delegate to JIT compiler stub
        return JitCompiler.compile(bytecode)
    }
}

class FakeWebOS : WebOS {
    override fun publishState(fragment: ByteArray) {}
    override fun fetchState(id: String): ByteArray? = null
}

class InfrastructureManagerTest {
    @Test
    fun `initialization wires components`() {
        val kernel = FakeKernel()
        InfrastructureManager.initialize(kernel, FakeSubOS(), FakeWebOS())
        assertNotNull(InfrastructureManager.secretKernel)
        assertNotNull(InfrastructureManager.subOs)
        assertNotNull(InfrastructureManager.webOs)
        assert(kernel.initialized)
    }
}
