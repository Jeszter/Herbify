package com.herbify.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herbify.app.game.GameState
import com.herbify.app.ui.theme.*

@Composable
fun ProfileScreen(gameState: GameState, onNavigateToEvents: () -> Unit, onNavigateToWallet: () -> Unit) {
    val eco by gameState.eco.collectAsState()
    val xp by gameState.xp.collectAsState()
    val level by gameState.level.collectAsState()
    val streak by gameState.streak.collectAsState()
    val collection by gameState.collection.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            // Avatar + name card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(NeonGreen.copy(alpha = 0.1f), CircleShape)
                            .border(2.dp, NeonGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌿", fontSize = 36.sp)
                    }
                    Text("Explorer", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .background(NeonGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("LEVEL $level", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
                    }

                    // XP bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("XP", color = TextSecondary, fontSize = 11.sp)
                            Text("${xp % (level * 100)} / ${level * 100}", color = TextSecondary, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = gameState.getXpProgress(),
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = NeonGreen,
                            trackColor = DarkSurface
                        )
                    }
                }
            }
        }

        item {
            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("💚", "$eco", "ECO Tokens", Modifier.weight(1f))
                StatCard("🔥", "$streak", "Day Streak", Modifier.weight(1f))
                StatCard("🌱", "${collection.size}", "Plants", Modifier.weight(1f))
            }
        }

        item {
            // Quick navigation
            Text("NAVIGATION", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }

        item {
            NavCard(
                emoji = "💰",
                title = "Wallet",
                subtitle = "$eco ECO available",
                onClick = onNavigateToWallet
            )
        }

        item {
            NavCard(
                emoji = "🌍",
                title = "Events",
                subtitle = "2 active events",
                onClick = onNavigateToEvents
            )
        }

        item {
            // Achievements preview
            Text("BADGES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val scanned = collection.size
                BadgeCard("🌱", "First Scan", scanned >= 1, Modifier.weight(1f))
                BadgeCard("🌿", "5 Plants", scanned >= 5, Modifier.weight(1f))
                BadgeCard("🌾", "10 Plants", scanned >= 10, Modifier.weight(1f))
                BadgeCard("🏆", "All Plants", scanned >= 10, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 22.sp)
            Text(value, color = NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
fun NavCard(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(emoji, fontSize = 28.sp)
                Column {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(subtitle, color = TextSecondary, fontSize = 12.sp)
                }
            }
            Text("›", color = NeonGreen, fontSize = 22.sp)
        }
    }
}

@Composable
fun BadgeCard(emoji: String, label: String, unlocked: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                if (unlocked) NeonGreen.copy(alpha = 0.08f) else DarkCard,
                RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                if (unlocked) NeonGreen.copy(alpha = 0.4f) else DarkBorder,
                RoundedCornerShape(10.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            emoji,
            fontSize = 22.sp,
            color = if (unlocked) Color.Unspecified else Color.Unspecified.copy(alpha = 0.3f)
        )
        Text(
            label,
            color = if (unlocked) TextPrimary else TextSecondary.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
