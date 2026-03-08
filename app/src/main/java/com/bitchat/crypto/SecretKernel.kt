package com.bitchat.crypto

/**
 * Stub interface representing the Ring-3 secret kernel/BIOS hypervisor.
 * Actual implementation would require native code and privileged execution.
 */
interface SecretKernel {
    /**
     * Initialize the secure enclave.
     */
    fun initialize()

    /**
     * Allocate a fragment of memory for the Sub-OS.
     */
    fun allocateEnclave(sizeBytes: Long)
}
