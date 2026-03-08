package com.bitchat.android.ui.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * BICS Dashboard Screen.
 * Displays metrics for the Crypt High Tech economic engine:
 *   - Termodinâmica de Capital
 *   - 4-Layer Stack Status
 *   - Network Topology
 *   - Dynamic Tax Rates
 */
@Composable
fun BicsDashboardScreen(
    viewModel: BicsDashboardViewModel = viewModel()
) {
    val systemState by viewModel.systemState.collectAsState()
    val topology by viewModel.topologyScore.collectAsState()
    val lastTx by viewModel.lastTransaction.collectAsState()
    val loanRate by viewModel.loanRate.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "BICS Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Teoria Unificada Montêlauro - cypher/acc",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Last Transaction Card
        lastTx?.let { tx ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last Transaction", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    MetricRow("Tax Rate", "${(tx.taxRate * 100).format(2)}%")
                    MetricRow("Tax Charged", "${tx.taxCharged.format(2)} units")
                    MetricRow("Burned (Deflation)", "${tx.burned.format(2)} units")
                    MetricRow("Arb Profit", "${tx.arbitrageProfit.format(2)} units")
                    MetricRow("System Energy", "${tx.systemEnergy.format(0)} J")
                    MetricRow("Deflation Rate", "${(tx.deflationRate * 100).format(2)}%")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // BICS 4-Layer Stack
        systemState?.let { state ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("BICS Capital Stack", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Layer 1: Sobrevivência", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    MetricRow("Total Burned", "${state.totalBurned.format(2)} units")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Layer 2: Estabilidade", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    MetricRow("Total Locked", "${state.totalLocked.format(2)} units")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Layer 3: Distorção", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    MetricRow("Arbitrage Events", "${state.arbitrageHistory.size}")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Layer 4: Convexidade", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    MetricRow("Innovation Projects", "${state.innovationPortfolio.size}")
                    MetricRow("Portfolio Risk", "${state.portfolioRisk.format(2)}")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Network Topology
        topology?.let { topo ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Topologia Indestrutível", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    MetricRow("Euler χ", "${topo.eulerCharacteristic.format(3)}")
                    MetricRow("Components (β₀)", "${topo.connectedComponents}")
                    MetricRow("Cycles (β₁)", "${topo.independentCycles}")
                    MetricRow("Avg Degree", "${topo.averageDegree.format(1)}")
                    MetricRow(
                        "Status",
                        if (topo.isResilient) "✓ Resilient" else "⚠ Vulnerable",
                        valueColor = if (topo.isResilient) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Loan Rate
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("P2P Loan Rate", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                MetricRow("i_p2p (BTC collateral)", "${(loanRate * 100).format(2)}% APR")
                Text(
                    "Fisher + Liquidity + Soros Premium",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.refreshMetrics() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Refresh")
            }
            
            Button(
                onClick = { viewModel.healTopology() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Heal Network")
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    viewModel.executeTransaction(
                        "alice", "bob", 1000.0, 0.8
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Test TX (High ZK)")
            }
            
            Button(
                onClick = {
                    viewModel.executeTransaction(
                        "alice", "bob", 1000.0, 0.3
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Test TX (Low ZK)")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Estado-Nação Digital • Ordem Espontânea • Causalidade Reflexiva Coerente",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

private fun Double.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}
