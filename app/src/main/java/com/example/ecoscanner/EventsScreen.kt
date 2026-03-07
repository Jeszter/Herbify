package com.example.ecoscanner

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecoscanner.ui.theme.*

data class GameEvent(
    val id: String, val emoji: String, val name: String,
    val description: String, val location: String,
    val daysLeft: Int, val multiplier: Int,
    val communityProgress: Float = 0f,
    val isFeatured: Boolean = false
)

val GAME_EVENTS = listOf(
    GameEvent("spring", "🔥", "Spring Bloom",
        "Copernicus detected a mass bloom in the Carpathians. Scan spring flowers before April 15 — ×10 tokens!",
        "Carpathians", 8, 10, communityProgress = 0.62f, isFeatured = true),
    GameEvent("volcanic", "🌋", "Volcanic Minerals",    "", "Kamchatka",  8,  5),
    GameEvent("seaweed",  "🐟", "Coastal Seaweed",      "", "Global",     3,  3),
    GameEvent("mushroom", "🍄", "Autumn Mushrooms",     "", "Siberia",    12, 4),
    GameEvent("arctic",   "❄️", "Arctic Moss",          "", "Arctic",     20, 6),
)

@Composable
fun EventsScreen() {
    LazyColumn(
        Modifier.fillMaxSize().background(EcoBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // ── LIVE badge + title ─────────────────────────────────────────────
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.background(Color(0xFF1A0A0A), RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFFFF4444).copy(.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(6.dp).background(Color(0xFFFF4444), CircleShape))
                        Text("LIVE  ·  2 active events", fontSize = 10.sp, color = Color(0xFFFF6666),
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Events", fontSize = 28.sp, fontWeight = FontWeight.Black, color = EcoTextPrimary)
            Spacer(Modifier.height(16.dp))
        }

        // ── Featured Event ─────────────────────────────────────────────────
        val featured = GAME_EVENTS.firstOrNull { it.isFeatured }
        if (featured != null) {
            item {
                FeaturedEventCard(featured)
                Spacer(Modifier.height(24.dp))
            }
        }

        // ── All events ────────────────────────────────────────────────────
        item {
            ProfileSectionLabel("ALL EVENTS")
            Spacer(Modifier.height(12.dp))
        }
        items(GAME_EVENTS.filter { !it.isFeatured }) { ev ->
            EventRow(ev)
            Spacer(Modifier.height(8.dp))
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun FeaturedEventCard(ev: GameEvent) {
    Box(
        Modifier.fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1A1030), Color(0xFF0E1A0E))),
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, EcoPurple.copy(.25f), RoundedCornerShape(20.dp))
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(ev.emoji, fontSize = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(ev.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = EcoTextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(ev.description, fontSize = 11.sp, color = EcoTextMuted, lineHeight = 16.sp)
            Spacer(Modifier.height(14.dp))

            // Community progress
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Community progress", fontSize = 10.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                Text("${(ev.communityProgress * 100).toInt()}%", fontSize = 11.sp,
                    color = EcoGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = ev.communityProgress,
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = EcoGreen, trackColor = EcoGreenDim.copy(.2f)
            )
            Spacer(Modifier.height(14.dp))

            // Rewards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RewardChip(Modifier.weight(1f), "🪙", "×${ev.multiplier} ECO")
                RewardChip(Modifier.weight(1f), "🃏", "Ltd. Card")
                RewardChip(Modifier.weight(1f), "🏆", "Achievement")
            }
            Spacer(Modifier.height(14.dp))

            // Button
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EcoPurple.copy(.7f), contentColor = Color.White)
            ) {
                Text("Participate  →", fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun RewardChip(modifier: Modifier, emoji: String, label: String) {
    Column(
        modifier.background(Color(0xFF0A1A12), RoundedCornerShape(12.dp))
            .border(1.dp, EcoBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(label, fontSize = 9.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun EventRow(ev: GameEvent) {
    Row(
        Modifier.fillMaxWidth()
            .background(EcoSurface, RoundedCornerShape(14.dp))
            .border(1.dp, EcoBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(44.dp).background(EcoGreenDim.copy(.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Text(ev.emoji, fontSize = 22.sp) }
        Column(Modifier.weight(1f)) {
            Text(ev.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoTextPrimary)
            Text("${ev.location}  ·  ${ev.daysLeft} days", fontSize = 10.sp,
                color = EcoTextMuted, fontFamily = FontFamily.Monospace)
        }
        Box(
            Modifier.background(EcoGreenDim.copy(.2f), RoundedCornerShape(8.dp))
                .border(1.dp, EcoGreen.copy(.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("×${ev.multiplier}", fontSize = 12.sp, color = EcoGreen, fontWeight = FontWeight.Black)
        }
    }
}