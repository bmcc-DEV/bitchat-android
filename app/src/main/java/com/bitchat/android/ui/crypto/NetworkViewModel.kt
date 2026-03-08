package com.bitchat.android.ui.crypto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitchat.crypto.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NetworkViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages

    init {
        NetworkService.registerListener { msg ->
            viewModelScope.launch {
                _messages.value = _messages.value + msg
            }
        }
    }

    fun sendMessage(msg: String) {
        viewModelScope.launch {
            NetworkService.broadcast(msg)
        }
    }
}