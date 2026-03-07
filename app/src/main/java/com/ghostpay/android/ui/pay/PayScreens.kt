package com.ghostpay.android.ui.pay

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Design tokens ──────────────────────────────────────────────────────────────

private val GhostCyan   = Color(0xFF00F5D4)
private val GhostViolet = Color(0xFF9B5DE5)
private val GhostAmber  = Color(0xFFFFBE0B)
private val GhostRed    = Color(0xFFFF006E)
private val SurfaceDark = Color(0xFF0D0F14)
private val CardDark    = Color(0xFF161B22)
private val CardBorder  = Color(0xFF30363D)

private val GhostGradient   = Brush.linearGradient(listOf(GhostCyan, GhostViolet))
private val WarningGradient = Brush.linearGradient(listOf(GhostAmber, GhostRed))

// ── Shared composables ─────────────────────────────────────────────────────────

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        content = { Column(Modifier.padding(16.dp), content = content) }
    )
}

@Composable
private fun GradientLabel(text: String) {
    Box(
        Modifier
            .background(GhostGradient, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

// ── Pay Dashboard ──────────────────────────────────────────────────────────────

/**
 * Main wallet overview screen.
 * Shows FNT-B/C/D balances, Pay-Bix, live i_p2p rate, and quick-action buttons.
 */
@Composable
fun PayDashboardScreen(
    fntBAmount: Double  = 0.0,
    fntCAmount: Double  = 0.0,
    fntDAmount: Double  = 0.0,
    payBixAmount: Double = 0.0,
    ipP2pRate: Double   = 0.06,
    onSend: () -> Unit  = {},
    onReceive: () -> Unit = {},
    onStake: () -> Unit = {},
    onBolsa: () -> Unit = {}
) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        label = "alpha", initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ghosT|Pay", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                color = GhostCyan, modifier = Modifier.weight(1f)
            )
            GradientLabel("v0.1.0-fantasma")
        }

        Spacer(Modifier.height(4.dp))

        // i_p2p live rate card
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Taxa i_p2p (rede local)", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "${"%.1f".format(ipP2pRate * 100)}% a.a.",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = GhostCyan.copy(alpha = alpha)
            )
            Text("Fisher + Liquidez + Prêmio Soros", color = Color.Gray, fontSize = 11.sp)
        }

        // Token balances
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TokenBalanceCard("FNT-B", fntBAmount, "Bearer", GhostCyan, Modifier.weight(1f))
            TokenBalanceCard("FNT-C", fntCAmount, "Commodity", GhostViolet, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TokenBalanceCard("FNT-D", fntDAmount, "Governance", GhostAmber, Modifier.weight(1f))
            TokenBalanceCard("PAY-BIX", payBixAmount, "Utility", GhostRed, Modifier.weight(1f))
        }

        // Action buttons
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Enviar", Icons.Default.Send, GhostCyan, onSend, Modifier.weight(1f))
            ActionButton("Receber", Icons.Default.Download, GhostViolet, onReceive, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Renda Fixa", Icons.Default.Lock, GhostAmber, onStake, Modifier.weight(1f))
            ActionButton("Dark Bolsa", Icons.Default.Storefront, GhostRed, onBolsa, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TokenBalanceCard(
    label: String, amount: Double, sublabel: String, color: Color, modifier: Modifier = Modifier
) {
    GlassCard(modifier) {
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("${"%.4f".format(amount)}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text(sublabel, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
private fun ActionButton(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CardDark, contentColor = tint),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.5f))
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Send / Receive ─────────────────────────────────────────────────────────────

/**
 * Payment screen with FX conversion preview.
 * Shows the seller's currency preference hint before confirming.
 */
@Composable
fun SendReceiveScreen(
    mode: SendReceiveMode = SendReceiveMode.SEND,
    fxHint: String?       = null,
    qrPayload: String?    = null,
    onBack: () -> Unit    = {},
    onConfirm: (Double, String, String) -> Unit = { _, _, _ -> }
) {
    var amount    by remember { mutableStateOf("") }
    var unit      by remember { mutableStateOf("un") }
    var commodity by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = GhostCyan) }
            Text(
                if (mode == SendReceiveMode.SEND) "Enviar Token" else "Receber Token",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp
            )
        }

        if (mode == SendReceiveMode.SEND) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Quantidade", color = Color.Gray, fontSize = 12.sp)
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("ex: 1.5") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GhostCyan, unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = unit, onValueChange = { unit = it },
                    label = { Text("Unidade (kg, L, h, un)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GhostCyan, unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = commodity, onValueChange = { commodity = it },
                    label = { Text("Commodity (ex: café arábica)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GhostCyan, unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )
            }

            fxHint?.let {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CurrencyExchange, null, tint = GhostAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Vendedor recebe: $it", color = GhostAmber, fontSize = 13.sp)
                    }
                }
            }

            Button(
                onClick = {
                    val a = amount.toDoubleOrNull() ?: return@Button
                    onConfirm(a, unit, commodity)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GhostCyan)
            ) {
                Icon(Icons.Default.Nfc, null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Aproximar & Pagar", color = Color.Black, fontWeight = FontWeight.Bold)
            }

        } else {
            // Receive mode: show QR
            GlassCard(Modifier.fillMaxWidth(), content = {
                Text("Seu QR Code de Recebimento", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        qrPayload ?: "gerando...",
                        color = Color.Black, fontSize = 10.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            })
        }
    }
}

enum class SendReceiveMode { SEND, RECEIVE }

// ── Dark Bolsa ─────────────────────────────────────────────────────────────────

data class AssetUiModel(
    val id: String, val name: String, val unit: String,
    val ultimatrixScore: Float, val isOwned: Boolean
)

@Composable
fun DarkBolsaScreen(
    assets: List<AssetUiModel> = emptyList(),
    onMintAsset: () -> Unit    = {},
    onAssetTap: (String) -> Unit = {}
) {
    Scaffold(
        containerColor = SurfaceDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onMintAsset,
                containerColor = GhostRed,
                shape = CircleShape
            ) {
                Icon(Icons.Default.AddAPhoto, null, tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Dark Bolsa", color = GhostRed, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text("Ativos tokenizados na rede mesh", color = Color.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
            }
            if (assets.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhum ativo ainda. Toque + para tokenizar.", color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(assets) { asset -> AssetCard(asset, onAssetTap) }
            }
        }
    }
}

@Composable
private fun AssetCard(asset: AssetUiModel, onTap: (String) -> Unit) {
    GlassCard(
        Modifier
            .fillMaxWidth()
            .clickable { onTap(asset.id) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(asset.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Unidade: ${asset.unit}", color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    Modifier
                        .background(
                            if (asset.ultimatrixScore >= 0.6f) GhostCyan else
                            if (asset.ultimatrixScore >= 0.2f) GhostAmber else GhostRed,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("U: ${"%.0f".format(asset.ultimatrixScore * 100)}",
                        color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (asset.isOwned) {
                    Spacer(Modifier.height(4.dp))
                    Text("Seu ativo", color = GhostCyan, fontSize = 10.sp)
                }
            }
        }
    }
}

// ── Staking (Renda Fixa Fantasma) ─────────────────────────────────────────────

data class StakingPositionUi(
    val positionId: String, val lockedAmount: Double, val unit: String,
    val accruedYield: Double, val unlockAt: Long
)

@Composable
fun StakingScreen(
    currentRate: Double              = 0.06,
    positions: List<StakingPositionUi> = emptyList(),
    onLock: (Double, String) -> Unit = { _, _ -> },
    onUnlock: (String) -> Unit       = {}
) {
    var lockAmount   by remember { mutableStateOf("") }
    var lockUnit     by remember { mutableStateOf("un") }
    var showLockForm by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Renda Fixa Fantasma", color = GhostAmber, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text("Taxa atual: ${"%.1f".format(currentRate * 100)}% a.a.", color = Color.Gray, fontSize = 13.sp)

        // Lock form toggle
        Button(
            onClick = { showLockForm = !showLockForm },
            colors = ButtonDefaults.buttonColors(containerColor = CardDark),
            border = BorderStroke(1.dp, GhostAmber),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Lock, null, tint = GhostAmber)
            Spacer(Modifier.width(8.dp))
            Text("Travar Saldo", color = GhostAmber, fontWeight = FontWeight.SemiBold)
        }

        if (showLockForm) {
            GlassCard(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = lockAmount, onValueChange = { lockAmount = it },
                    label = { Text("Quantidade") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GhostAmber, unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = lockUnit, onValueChange = { lockUnit = it },
                    label = { Text("Unidade") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GhostAmber, unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val a = lockAmount.toDoubleOrNull() ?: return@Button
                        onLock(a, lockUnit)
                        showLockForm = false
                        lockAmount = ""; lockUnit = "un"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GhostAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirmar Travamento", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Positions list
        if (positions.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text("Nenhuma posição aberta.", color = Color.Gray)
            }
        } else {
            positions.forEach { pos ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${"%.4f".format(pos.lockedAmount)} ${pos.unit}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Rendimento: + ${"%.4f".format(pos.accruedYield)}", color = GhostCyan, fontSize = 12.sp)
                        }
                        IconButton(onClick = { onUnlock(pos.positionId) }) {
                            Icon(Icons.Default.LockOpen, null, tint = GhostAmber)
                        }
                    }
                }
            }
        }
    }
}
