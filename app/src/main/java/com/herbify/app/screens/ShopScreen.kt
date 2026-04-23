package com.herbify.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herbify.app.game.GameState
import com.herbify.app.game.ShopItem
import com.herbify.app.game.ShopItemType
import com.herbify.app.ui.theme.*

@Composable
fun ShopScreen(gameState: GameState) {
    val eco by gameState.eco.collectAsState()
    val ownedItems by gameState.ownedItems.collectAsState()
    var purchaseMessage by remember { mutableStateOf<String?>(null) }

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
                Text("🛒 SHOP", color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Upgrades & Boosters", color = TextSecondary, fontSize = 13.sp)
            }
            Row(
                modifier = Modifier
                    .background(DarkCard, RoundedCornerShape(12.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("💚", fontSize = 16.sp)
                Text("$eco ECO", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Purchase message
        purchaseMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2000)
                purchaseMessage = null
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeonGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .padding(bottom = 8.dp)
            ) {
                Text(msg, color = NeonGreen, fontSize = 13.sp)
            }
        }

        // Sections
        Text("UPGRADES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
            modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(gameState.shopItems.filter { it.type == ShopItemType.UPGRADE }) { item ->
                ShopItemCard(
                    item = item,
                    isOwned = ownedItems.contains(item.id),
                    canAfford = eco >= item.cost,
                    onBuy = {
                        val bought = gameState.buyItem(item)
                        purchaseMessage = if (bought) "✅ ${item.name} purchased!" else "❌ Not enough ECO!"
                    }
                )
            }
            item {
                Text("BOOSTERS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            }
            items(gameState.shopItems.filter { it.type == ShopItemType.BOOSTER }) { item ->
                ShopItemCard(
                    item = item,
                    isOwned = ownedItems.contains(item.id),
                    canAfford = eco >= item.cost,
                    onBuy = {
                        val bought = gameState.buyItem(item)
                        purchaseMessage = if (bought) "✅ ${item.name} activated!" else "❌ Not enough ECO!"
                    }
                )
            }
        }
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    isOwned: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit
) {
    val borderColor = when {
        isOwned -> NeonGreen.copy(alpha = 0.5f)
        canAfford -> NeonGreen.copy(alpha = 0.2f)
        else -> DarkBorder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(item.emoji, fontSize = 32.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(item.description, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
            if (isOwned) {
                Box(
                    modifier = Modifier
                        .background(NeonGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("✓ OWNED", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onBuy,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        disabledContainerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BUY", color = DarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${item.cost} ECO", color = DarkBg.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
