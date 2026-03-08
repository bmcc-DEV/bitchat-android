package com.bitchat.android.ui.crypto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitchat.crypto.EdgeOracle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OracleViewModel : ViewModel() {
    private val _price = MutableStateFlow<String>("")
    val price: StateFlow<String> = _price

    fun query(symbol: String) {
        viewModelScope.launch {
            _price.value = EdgeOracle.fetchRealWorldData(symbol)
        }
    }
}
