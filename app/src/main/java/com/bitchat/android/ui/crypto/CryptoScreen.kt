package com.bitchat.android.ui.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CryptoScreen(viewModel: CryptoViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ledger", "BICS Dashboard")
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        when (selectedTab) {
            0 -> LedgerTab(viewModel)
            1 -> BicsDashboardScreen()
        }
    }
}

@Composable
private fun LedgerTab(viewModel: CryptoViewModel) {
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
        if (history.isNotEmpty()) {
            Text(toSparkline(history), style = MaterialTheme.typography.bodySmall)
        }
        history.forEach { u ->
            Text(u.toString())
        }
    }
}

private fun toSparkline(values: List<Double>): String {
    if (values.isEmpty()) return ""
    val bars = ".:-=+*#%@"
    val min = values.minOrNull() ?: return ""
    val max = values.maxOrNull() ?: return ""
    if (min == max) return bars.first().toString().repeat(values.size)
    return buildString {
        values.forEach { v ->
            val normalized = ((v - min) / (max - min)).coerceIn(0.0, 1.0)
            val index = (normalized * (bars.length - 1)).toInt()
            append(bars[index])
        }
    }
}
