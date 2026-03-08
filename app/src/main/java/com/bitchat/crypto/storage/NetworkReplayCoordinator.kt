package com.bitchat.crypto.storage

import android.content.Context
import com.bitchat.crypto.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object NetworkReplayCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var started = false

    fun start(context: Context, replayLimit: Int = 200) {
        if (started) return
        started = true

        val dao = CryptoOpsDatabase.get(context).networkLogDao()
        NodeStateRepository.init(dao)

        // Persist every ordered gossip message.
        NetworkService.registerOrderedListener { msg ->
            scope.launch {
                dao.insert(
                    NetworkLogEntry(
                        messageId = msg.id,
                        sequence = msg.sequence,
                        payload = msg.payload,
                        timestampMs = System.currentTimeMillis()
                    )
                )
            }
        }

        // Replay latest ordered messages at startup.
        scope.launch {
            dao.latest(replayLimit).forEach { entry ->
                NetworkService.broadcastOrdered(
                    NetworkService.NetworkMessage(
                        id = entry.messageId,
                        sequence = entry.sequence,
                        payload = entry.payload
                    )
                )
            }
        }
    }
}
