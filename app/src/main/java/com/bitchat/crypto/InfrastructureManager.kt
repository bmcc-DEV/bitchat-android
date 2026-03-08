package com.bitchat.crypto

/**
 * Orchestrates initialization of the various low-level components such as
 * SecretKernel, SubOS and WebOS. In this stubbed version we simply log their
 * creation and keep references for later use.
 */
object InfrastructureManager {
    var secretKernel: SecretKernel? = null
    var subOs: SubOS? = null
    var webOs: WebOS? = null

    fun initialize(
        kernel: SecretKernel,
        sub: SubOS,
        web: WebOS
    ) {
        secretKernel = kernel
        subOs = sub
        webOs = web
        kernel.initialize()
        // allocate a small enclave as demonstration
        kernel.allocateEnclave(1024)
        println("InfrastructureManager: initialized components")
    }
}
