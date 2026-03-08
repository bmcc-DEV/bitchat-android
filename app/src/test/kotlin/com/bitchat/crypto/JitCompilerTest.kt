package com.bitchat.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class JitCompilerTest {
    @Test
    fun `compile adds marker prefix`() {
        val input = "code".toByteArray()
        val output = JitCompiler.compile(input)
        val expected = "JIT:code".toByteArray()
        assertArrayEquals(expected, output)
    }
}
