package com.bitchat.android.ui.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OracleScreen(viewModel: OracleViewModel = viewModel()) {
    val price by viewModel.price.collectAsState()
    var symbol by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Oracle Query")
        OutlinedTextField(
            value = symbol,
            onValueChange = { symbol = it },
            label = { Text("Symbol") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.query(symbol) }) {
            Text("Fetch")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(price)
    }
}
