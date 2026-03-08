package com.bitchat.android.ui.crypto

import androidx.lifecycle.ViewModel
import com.bitchat.crypto.WalletAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WalletAuthViewModel : ViewModel() {
    private val _status = MutableStateFlow("Not authenticated")
    val status: StateFlow<String> = _status

    fun register(pin: String) {
        runCatching {
            WalletAuthManager.registerPin(pin)
            _status.value = "PIN registered"
        }.onFailure {
            _status.value = "Register failed"
        }
    }

    fun login(pin: String) {
        val ok = WalletAuthManager.authenticate(pin)
        _status.value = if (ok) {
            "Authenticated: ${WalletAuthManager.currentSessionToken()?.take(8)}..."
        } else {
            "Auth failed"
        }
    }
}
