package com.bitchat.crypto

import com.bitchat.android.net.OkHttpProvider
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Optional real transport bridge for gossip traffic over WebSocket.
 */
class WebSocketGossipTransport(
    private val relayUrl: String,
    private val onMessage: (String) -> Unit
) {
    private var webSocket: WebSocket? = null

    fun connect() {
        val request = Request.Builder().url(relayUrl).build()
        webSocket = OkHttpProvider.webSocketClient().newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                CryptoMetrics.inc("gossip.ws.open")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                CryptoMetrics.inc("gossip.ws.inbound")
                onMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                CryptoMetrics.inc("gossip.ws.closed")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                CryptoMetrics.inc("gossip.ws.failure")
            }
        })
    }

    fun send(text: String): Boolean {
        return webSocket?.send(text) ?: false
    }

    fun disconnect() {
        webSocket?.close(1000, "shutdown")
        webSocket = null
    }
}
