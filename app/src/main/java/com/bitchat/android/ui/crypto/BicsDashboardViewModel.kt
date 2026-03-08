package com.bitchat.android.ui.crypto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitchat.crypto.CryptoLedger
import com.bitchat.crypto.bics.BicsCoordinator
import com.bitchat.crypto.topology.TopologyGuardian
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for BICS Dashboard.
 * Displays real-time metrics of the cypher/acc economic engine.
 */
class BicsDashboardViewModel : ViewModel() {
    
    private val ledger = CryptoLedger(taxRate = 0.05)
    private val coordinator = BicsCoordinator(ledger)
    private val topology = TopologyGuardian()
    
    private val _systemState = MutableStateFlow<BicsCoordinator.SystemState?>(null)
    val systemState: StateFlow<BicsCoordinator.SystemState?> = _systemState
    
    private val _topologyScore = MutableStateFlow<TopologyGuardian.RobustnessScore?>(null)
    val topologyScore: StateFlow<TopologyGuardian.RobustnessScore?> = _topologyScore
    
    private val _lastTransaction = MutableStateFlow<BicsCoordinator.TransactionResult?>(null)
    val lastTransaction: StateFlow<BicsCoordinator.TransactionResult?> = _lastTransaction
    
    private val _loanRate = MutableStateFlow(0.0)
    val loanRate: StateFlow<Double> = _loanRate
    
    init {
        // Initialize topology with sample nodes
        initializeTopology()
        // Refresh metrics
        refreshMetrics()
    }
    
    private fun initializeTopology() {
        viewModelScope.launch {
            for (i in 1..10) {
                topology.addNode("node$i")
            }
            
            // Create mesh connections
            for (i in 1..9) {
                topology.addEdge("node$i", "node${i+1}")
            }
            topology.addEdge("node10", "node1")
            
            // Add redundancy
            topology.reconfigureMeshTopology(targetRedundancy = 3)
            
            _topologyScore.value = topology.calculateRobustness()
        }
    }
    
    fun executeTransaction(from: String, to: String, amount: Double, zkStrength: Double = 0.7) {
        viewModelScope.launch {
            val result = coordinator.processTransaction(from, to, amount, zkStrength)
            _lastTransaction.value = result
            refreshMetrics()
        }
    }
    
    fun refreshMetrics() {
        viewModelScope.launch {
            _systemState.value = coordinator.getSystemState()
            _topologyScore.value = topology.calculateRobustness()
            _loanRate.value = coordinator.calculateLoanRate(
                collateralAsset = "BTC",
                collateralVolatility = 0.25,
                requestedAmount = 1000.0
            )
        }
    }
    
    fun healTopology() {
        viewModelScope.launch {
            topology.healPartitions()
            _topologyScore.value = topology.calculateRobustness()
        }
    }
    
    fun simulateAttack(percentage: Double = 0.3): TopologyGuardian.AttackSimulation {
        return topology.simulateAttack(percentage)
    }
    
    override fun onCleared() {
        super.onCleared()
        coordinator.shutdown()
    }
}
