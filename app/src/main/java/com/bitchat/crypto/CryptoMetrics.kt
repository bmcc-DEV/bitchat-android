package com.bitchat.crypto

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object CryptoMetrics {
    private val counters = ConcurrentHashMap<String, AtomicLong>()

    fun inc(name: String, delta: Long = 1) {
        counters.getOrPut(name) { AtomicLong(0) }.addAndGet(delta)
    }

    fun get(name: String): Long = counters[name]?.get() ?: 0L

    fun snapshot(): Map<String, Long> = counters.mapValues { it.value.get() }
}
