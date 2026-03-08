package com.bitchat.crypto

/**
 * Web-OS interface stub. In the full system it would manage a floating memory
 * state shared across many nodes.
 */
interface WebOS {
    /**
     * Publish a state fragment to the global mesh.
     */
    fun publishState(fragment: ByteArray)

    /**
     * Retrieve a state fragment by identifier.
     */
    fun fetchState(id: String): ByteArray?
}
