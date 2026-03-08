package com.bitchat.android.ui.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CryptoScreen(viewModel: CryptoViewModel = viewModel()) {
    val balances by viewModel.balances.collectAsState()
    val history by viewModel.history.collectAsState()
    var from by remember { mutableStateOf("alice") }
    var to by remember { mutableStateOf("bob") }
    var amount by remember { mutableStateOf(0.0) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Balances:")
        balances.forEach { (acct, bal) ->
            Text("$acct: $bal")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = from,
            onValueChange = { from = it },
            label = { Text("From") }
        )
        OutlinedTextField(
            value = to,
            onValueChange = { to = it },
            label = { Text("To") }
        )
        OutlinedTextField(
            value = if (amount == 0.0) "" else amount.toString(),
            onValueChange = { amount = it.toDoubleOrNull() ?: 0.0 },
            label = { Text("Amount") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.transfer(from, to, amount) }) {
            Text("Transfer")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Unified value history:")
        history.forEach { u ->
            Text(u.toString())
        }
    }
}
