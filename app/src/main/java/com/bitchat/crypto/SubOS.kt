package com.bitchat.crypto

/**
 * Represents the state-distributed Sub-OS ("Nó Simbionte").
 * The real implementation would involve a dynamic compiler/JIT.
 */
interface SubOS {
    /**
     * Execute a transaction or instruction within the Sub-OS context.
     */
    fun executeInstruction(instruction: ByteArray)

    /**
     * Cross-compile P2P network bytecode to native architecture.
     */
    fun compileNetworkCode(bytecode: ByteArray): ByteArray
}
