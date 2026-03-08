package com.bitchat.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitchat.crypto.CryptoLedger
import kotlinx.coroutines.launch

class CryptoViewModel : ViewModel() {
    private val ledger = CryptoLedger()
    var balanceAlice by mutableStateOf(0.0)
        private set
    var balanceBob by mutableStateOf(0.0)
        private set

    fun depositAlice(amount: Double) {
        ledger.deposit("alice", amount)
        refreshBalances()
    }

    fun transferAliceToBob(amount: Double) {
        ledger.transfer("alice", "bob", amount)
        refreshBalances()
    }

    private fun refreshBalances() {
        balanceAlice = ledger.getBalance("alice")
        balanceBob = ledger.getBalance("bob")
    }
}

@Composable
fun CryptoScreen(viewModel: CryptoViewModel) {
    var depositAmount by remember { mutableStateOf(0.0) }
    var transferAmount by remember { mutableStateOf(0.0) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Alice balance: ${viewModel.balanceAlice}")
        Text("Bob balance: ${viewModel.balanceBob}")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = if (depositAmount == 0.0) "" else depositAmount.toString(),
            onValueChange = { depositAmount = it.toDoubleOrNull() ?: 0.0 },
            label = { Text("Deposit to Alice") }
        )
        Button(onClick = { viewModel.depositAlice(depositAmount) }) {
            Text("Deposit")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = if (transferAmount == 0.0) "" else transferAmount.toString(),
            onValueChange = { transferAmount = it.toDoubleOrNull() ?: 0.0 },
            label = { Text("Transfer from Alice to Bob") }
        )
        Button(onClick = { viewModel.transferAliceToBob(transferAmount) }) {
            Text("Transfer")
        }
    }
}
