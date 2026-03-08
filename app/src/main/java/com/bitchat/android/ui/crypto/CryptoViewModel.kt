package com.bitchat.android.ui.crypto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitchat.crypto.CryptoLedger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CryptoViewModel : ViewModel() {
    private val ledger = CryptoLedger()

    private val _balances = MutableStateFlow<Map<String, Double>>(emptyMap())
    val balances: StateFlow<Map<String, Double>> = _balances

    private val _history = MutableStateFlow<List<Double>>(emptyList())
    val history: StateFlow<List<Double>> = _history

    init {
        // initial accounts
        ledger.deposit("alice", 1000.0)
        ledger.deposit("bob", 500.0)
        updateBalances()
    }

    private fun updateBalances() {
        _balances.value = mapOf(
            "alice" to ledger.getBalance("alice"),
            "bob" to ledger.getBalance("bob")
        )
    }

    fun transfer(from: String, to: String, amount: Double) {
        viewModelScope.launch {
            if (ledger.transfer(from, to, amount)) {
                updateBalances()
                _history.value = ledger.getUnifiedHistory()
            }
        }
    }

    fun deposit(account: String, amount: Double) {
        viewModelScope.launch {
            ledger.deposit(account, amount)
            updateBalances()
        }
    }
}
