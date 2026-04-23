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
import com.herbify.app.game.EventItem
import com.herbify.app.game.GameState
import com.herbify.app.ui.theme.*

@Composable
fun EventsScreen(gameState: GameState, onBack: () -> Unit) {
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
                Text("🌍 EVENTS", color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Live & Upcoming", color = TextSecondary, fontSize = 13.sp)
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

        // Active badge
        Box(
            modifier = Modifier
                .background(NeonGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .padding(bottom = 8.dp)
        ) {
            Text("🔴  2 events live now", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(gameState.events) { event ->
                EventCard(event)
            }
        }
    }
}

@Composable
fun EventCard(event: EventItem) {
    val borderColor = if (event.isActive) NeonGreen.copy(alpha = 0.4f) else DarkBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(event.emoji, fontSize = 30.sp)
                    Column {
                        Text(event.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (event.isActive) {
                            Text("🔴 LIVE", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        } else {
                            Text("Coming soon", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(event.description, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .background(EcoGold.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .border(1.dp, EcoGold.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🏆", fontSize = 14.sp)
                Text("Reward: ${event.reward}", color = EcoGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
