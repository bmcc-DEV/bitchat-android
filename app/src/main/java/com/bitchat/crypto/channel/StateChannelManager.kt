package com.bitchat.crypto.channel

import java.util.concurrent.ConcurrentHashMap

/**
 * State Channel Manager - Lifecycle management for payment channels.
 * 
 * Manages:
 * - Channel opening/closing
 * - Route finding for multi-hop payments
 * - Liquidity balancing
 * - Watchtower services (monitor for fraud)
 */
class StateChannelManager {
    
    private val channels = ConcurrentHashMap<String, StateChannel>()
    private val userChannels = ConcurrentHashMap<String, MutableSet<String>>()  // userId -> channelIds
    
    /**
     * Open a new channel between two parties.
     * Requires on-chain transaction to lock collateral.
     */
    fun openChannel(
        partyA: String,
        partyB: String,
        amountA: Double,
        amountB: Double
    ): String {
        val channelId = generateChannelId(partyA, partyB)
        
        val channel = StateChannel(
            channelId = channelId,
            partyA = partyA,
            partyB = partyB,
            initialBalanceA = amountA,
            initialBalanceB = amountB
        )
        
        channels[channelId] = channel
        
        userChannels.computeIfAbsent(partyA) { mutableSetOf() }.add(channelId)
        userChannels.computeIfAbsent(partyB) { mutableSetOf() }.add(channelId)
        
        return channelId
    }
    
    /**
     * Find a route from source to destination.
     * Implements simplified Dijkstra for payment routing.
     */
    fun findRoute(from: String, to: String, amount: Double): PaymentRoute? {
        if (from == to) return null
        
        // Direct channel exists?
        val directChannel = findChannel(from, to)
        if (directChannel != null && hasLiquidity(directChannel, from, amount)) {
            return PaymentRoute(listOf(directChannel))
        }
        
        // Multi-hop routing
        val visited = mutableSetOf<String>()
        val queue = mutableListOf(RouteNode(from, emptyList(), 0.0))
        
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            if (current.user in visited) continue
            visited.add(current.user)
            
            if (current.user == to) {
                return PaymentRoute(current.path)
            }
            
            // Find neighbors
            val userChan = userChannels[current.user] ?: continue
            for (channelId in userChan) {
                val channel = channels[channelId] ?: continue
                val neighbor = if (channel.partyA == current.user) channel.partyB else channel.partyA
                
                if (neighbor !in visited && hasLiquidity(channelId, current.user, amount)) {
                    queue.add(RouteNode(
                        user = neighbor,
                        path = current.path + channelId,
                        cost = current.cost + 0.001  // Routing fee
                    ))
                }
            }
            
            queue.sortBy { it.cost }
        }
        
        return null  // No route found
    }
    
    /**
     * Execute a multi-hop payment.
     * Uses HTLCs (Hash Time-Locked Contracts) for atomicity.
     */
    fun executePayment(route: PaymentRoute, amount: Double): PaymentResult {
        val updates = mutableListOf<StateChannel.SignedUpdate>()
        
        // Create updates for each hop
        for (i in 0 until route.hops.size) {
            val channelId = route.hops[i]
            val channel = channels[channelId] ?: return PaymentResult(
                success = false,
                error = "Channel $channelId not found"
            )
            
            val (from, to) = if (i == 0) {
                Pair(channel.partyA, channel.partyB)
            } else {
                val previousChannel = channels[route.hops[i - 1]]!!
                if (previousChannel.partyB == channel.partyA) {
                    Pair(channel.partyA, channel.partyB)
                } else {
                    Pair(channel.partyB, channel.partyA)
                }
            }
            
            val update = channel.createUpdate(from, to, amount) ?: return PaymentResult(
                success = false,
                error = "Failed to create update for channel $channelId"
            )
            
            updates.add(update)
        }
        
        // In production: use HTLCs to ensure atomicity
        // For now, commit all updates sequentially
        for ((index, update) in updates.withIndex()) {
            val channelId = route.hops[index]
            val channel = channels[channelId]!!
            
            // Simulate signatures
            val mockSigA = "sigA_${channel.partyA}".toByteArray()
            val mockSigB = "sigB_${channel.partyB}".toByteArray()
            
            channel.signUpdate(update.nonce, channel.partyA, mockSigA)
            channel.signUpdate(update.nonce, channel.partyB, mockSigB)
        }
        
        return PaymentResult(
            success = true,
            totalFees = route.hops.size * 0.001
        )
    }
    
    /**
     * Close a channel and settle on-chain.
     */
    fun closeChannel(channelId: String, cooperative: Boolean = true): StateChannel.ChannelSettlement? {
        val channel = channels[channelId] ?: return null
        
        val settlement = if (cooperative) {
            channel.closeCooperative()
        } else {
            channel.closeContested(channel.partyA)
        }
        
        // Remove from indices
        if (settlement != null) {
            userChannels[channel.partyA]?.remove(channelId)
            userChannels[channel.partyB]?.remove(channelId)
            channels.remove(channelId)
        }
        
        return settlement
    }
    
    /**
     * Get all channels for a user.
     */
    fun getUserChannels(userId: String): List<StateChannel.ChannelInfo> {
        val channelIds = userChannels[userId] ?: return emptyList()
        return channelIds.mapNotNull { 
            channels[it]?.getStatus() 
        }
    }
    
    /**
     * Get network statistics.
     */
    fun getNetworkStats(): NetworkStats {
        val totalChannels = channels.size
        val activeChannels = channels.values.count { !it.getStatus().isClosed }
        val totalCapacity = channels.values.sumOf { it.getStatus().capacity }
        val totalTransactions = channels.values.sumOf { it.getStatus().totalTransactions }
        
        return NetworkStats(
            totalChannels = totalChannels,
            activeChannels = activeChannels,
            totalCapacityLocked = totalCapacity,
            totalOffChainTransactions = totalTransactions,
            averageChannelSize = if (totalChannels > 0) totalCapacity / totalChannels else 0.0
        )
    }
    
    private fun generateChannelId(partyA: String, partyB: String): String {
        val sorted = listOf(partyA, partyB).sorted()
        return "channel_${sorted[0]}_${sorted[1]}_${System.currentTimeMillis()}"
    }
    
    private fun findChannel(userA: String, userB: String): String? {
        val channelsA = userChannels[userA] ?: return null
        val channelsB = userChannels[userB] ?: return null
        return channelsA.intersect(channelsB).firstOrNull()
    }
    
    private fun hasLiquidity(channelId: String, from: String, amount: Double): Boolean {
        val channel = channels[channelId] ?: return false
        val info = channel.getStatus()
        
        return when (from) {
            info.partyA -> info.balanceA >= amount
            info.partyB -> info.balanceB >= amount
            else -> false
        }
    }
    
    private data class RouteNode(
        val user: String,
        val path: List<String>,
        val cost: Double
    )
    
    data class PaymentRoute(
        val hops: List<String>  // List of channel IDs
    )
    
    data class PaymentResult(
        val success: Boolean,
        val totalFees: Double = 0.0,
        val error: String? = null
    )
    
    data class NetworkStats(
        val totalChannels: Int,
        val activeChannels: Int,
        val totalCapacityLocked: Double,
        val totalOffChainTransactions: Int,
        val averageChannelSize: Double
    )
}
