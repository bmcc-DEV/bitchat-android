package com.bitchat.android.ui.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NetworkScreen(viewModel: NetworkViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    var newMsg by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Network messages:")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                Text(msg)
            }
        }
        OutlinedTextField(
            value = newMsg,
            onValueChange = { newMsg = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            viewModel.sendMessage(newMsg)
            newMsg = ""
        }) {
            Text("Send")
        }
    }
}
