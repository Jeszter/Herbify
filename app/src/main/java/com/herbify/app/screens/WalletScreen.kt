package com.herbify.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herbify.app.game.GameState
import com.herbify.app.ui.theme.*

data class Transaction(val label: String, val amount: Int, val emoji: String)

@Composable
fun WalletScreen(gameState: GameState, onBack: () -> Unit) {
    val eco by gameState.eco.collectAsState()
    val collection by gameState.collection.collectAsState()

    // Build a simple transaction log from collection
    val transactions = collection.map { plant ->
        Transaction("+${plant.name} scanned", plant.ecoReward, plant.emoji)
    }.reversed().take(10)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("💰 WALLET", color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Your ECO balance", color = TextSecondary, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("← Back")
            }
        }

        // Balance card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("💚", fontSize = 48.sp)
                Text("$eco ECO", color = NeonGreen, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text("Total Balance", color = TextSecondary, fontSize = 13.sp)
                Divider(color = DarkBorder, modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${collection.sumOf { it.ecoReward }}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Total Earned", color = TextSecondary, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${collection.sumOf { it.ecoReward } - eco}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Total Spent", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("RECENT TRANSACTIONS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions yet. Start scanning plants!", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions.size) { i ->
                    val tx = transactions[i]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, RoundedCornerShape(10.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(tx.emoji, fontSize = 22.sp)
                            Text(tx.label, color = TextPrimary, fontSize = 13.sp)
                        }
                        Text("+${tx.amount} ECO", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
