package com.bitchat.crypto

/**
 * Emulates isolated enclave pages in-process.
 */
class EnclaveMemoryManager {
    private val pages = mutableMapOf<String, ByteArray>()

    fun allocatePage(pageId: String, size: Int) {
        require(size > 0) { "size must be positive" }
        pages[pageId] = ByteArray(size)
        CryptoMetrics.inc("enclave.pages.allocated")
    }

    fun write(pageId: String, offset: Int, data: ByteArray) {
        val page = pages[pageId] ?: return
        if (offset < 0 || offset + data.size > page.size) return
        data.copyInto(page, destinationOffset = offset)
        CryptoMetrics.inc("enclave.pages.write")
    }

    fun read(pageId: String, offset: Int, length: Int): ByteArray {
        val page = pages[pageId] ?: return ByteArray(0)
        if (offset < 0 || length < 0 || offset + length > page.size) return ByteArray(0)
        CryptoMetrics.inc("enclave.pages.read")
        return page.copyOfRange(offset, offset + length)
    }
}
