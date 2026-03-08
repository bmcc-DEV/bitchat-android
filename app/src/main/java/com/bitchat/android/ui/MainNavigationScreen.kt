package com.bitchat.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitchat.android.ui.crypto.NetworkScreen
import com.bitchat.android.ui.crypto.OracleScreen
import com.ghostpay.android.ui.pay.PayDashboardScreen
import com.bitchat.android.ui.crypto.CryptoScreen

/**
 * Main navigation container with bottom navigation bar
 * Provides tabs for Chat and Pay functionality
 */
@Composable
fun MainNavigationScreen(chatViewModel: ChatViewModel) {
    var selectedTab by remember { mutableStateOf(NavigationTab.CHAT) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                NavigationTab.CHAT -> ChatScreen(viewModel = chatViewModel)
                NavigationTab.PAY -> PayDashboardScreen()
                NavigationTab.CRYPTO -> CryptoScreen()
                NavigationTab.NETWORK -> NetworkScreen()
                NavigationTab.ORACLE -> OracleScreen()
            }
        }
    }
}

/**
 * Navigation tabs enum
 */
enum class NavigationTab(val title: String, val icon: ImageVector) {
    CHAT("Chat", Icons.Filled.Chat),
    PAY("Pay", Icons.Filled.AccountBalanceWallet),
    CRYPTO("Crypto", Icons.Filled.CurrencyBitcoin),
    NETWORK("Network", Icons.Filled.Wifi),
    ORACLE("Oracle", Icons.Filled.Lightbulb)
}
