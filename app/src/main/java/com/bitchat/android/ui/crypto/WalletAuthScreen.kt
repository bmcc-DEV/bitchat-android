package com.bitchat.android.ui.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WalletAuthScreen(viewModel: WalletAuthViewModel = viewModel()) {
    val status by viewModel.status.collectAsState()
    var pin by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Wallet Authentication")
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("PIN") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.register(pin) }) { Text("Register") }
            Button(onClick = { viewModel.login(pin) }) { Text("Login") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(status)
    }
}
