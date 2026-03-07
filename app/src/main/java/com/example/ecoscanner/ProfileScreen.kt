package com.example.ecoscanner

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecoscanner.ui.theme.*

@Composable
fun ProfileScreen(onOpenEvents: () -> Unit, onOpenWallet: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(EcoBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // ── Hero ──────────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.size(72.dp).background(EcoGreenDim.copy(.35f), CircleShape)
                    .border(2.dp, EcoGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("🌿", fontSize = 32.sp) }
            Spacer(Modifier.height(12.dp))
            Text("NaturaHunter", fontSize = 28.sp, fontWeight = FontWeight.Black, color = EcoTextPrimary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("🌿", fontSize = 11.sp)
                Text("ЭКО-НАТУРАЛИСТ  ·  ТОП 142", fontSize = 11.sp, color = EcoGreen,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.background(EcoGreenDim.copy(.2f), RoundedCornerShape(20.dp))
                    .border(1.dp, EcoGreen.copy(.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⭐", fontSize = 13.sp)
                    Text("Уровень ${GameState.level}  ·  ${GameState.levelTitle}", fontSize = 12.sp,
                        color = EcoGold, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("ХР до Уровня ${GameState.level + 1}", fontSize = 10.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                Text("${GameState.xpForCurrent} / 2000", fontSize = 10.sp, color = EcoGreen,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = GameState.xpProgress,
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                color = EcoGreen, trackColor = EcoGreenDim.copy(.2f)
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Быстрый доступ ────────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileNavCard(
                    modifier  = Modifier.weight(1f),
                    emoji     = "🪙",
                    title     = "Кошелёк",
                    subtitle  = "${GameState.ecoBalance} ECO",
                    onClick   = onOpenWallet
                )
                ProfileNavCard(
                    modifier  = Modifier.weight(1f),
                    emoji     = "🌍",
                    title     = "Ивенты",
                    subtitle  = "2 активных",
                    onClick   = onOpenEvents
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Ежедневный стрик ─────────────────────────────────────────────────
        item {
            ProfileSectionLabel("ЕЖЕДНЕВНЫЙ СТРИК")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreakCard(Modifier.weight(1f), "🔥", "${GameState.streak}", "Дней подряд", Color(0xFFFF8C00))
                StreakCard(Modifier.weight(1f), null, "×${"%.1f".format(GameState.streakMultiplier)}", "Бонус токенов", EcoGreen)
                StreakCard(Modifier.weight(1f), null, "${GameState.ecoBalance}", "ECO всего", EcoGold)
            }
            Spacer(Modifier.height(10.dp))
            val days = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                days.forEachIndexed { i, d ->
                    val active = i == 6
                    Box(
                        Modifier.weight(1f)
                            .background(if (active) EcoGold.copy(.15f) else EcoSurface, RoundedCornerShape(10.dp))
                            .border(1.dp, if (active) EcoGold.copy(.5f) else EcoBorder, RoundedCornerShape(10.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(d, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) EcoGold else EcoTextMuted, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Квесты ────────────────────────────────────────────────────────────
        item {
            ProfileSectionLabel("ДНЕВНЫЕ КВЕСТЫ")
            Spacer(Modifier.height(12.dp))
        }
        items(DAILY_QUESTS) { quest ->
            QuestRow(quest)
            Spacer(Modifier.height(8.dp))
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun StreakCard(modifier: Modifier, icon: String?, value: String, label: String, valueColor: Color) {
    Box(modifier.background(EcoSurface, RoundedCornerShape(14.dp)).border(1.dp, EcoBorder, RoundedCornerShape(14.dp))
        .padding(vertical = 14.dp, horizontal = 8.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) { Text(icon, fontSize = 18.sp); Spacer(Modifier.width(3.dp)) }
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = valueColor)
            }
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 9.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center, lineHeight = 12.sp)
        }
    }
}

@Composable
fun QuestRow(quest: DailyQuest) {
    val progress  = GameState.questProgress[quest.id] ?: 0
    val done      = GameState.isQuestDone(quest.id)
    val progFloat = GameState.questProgressFloat(quest.id)
    Row(
        Modifier.fillMaxWidth().background(EcoSurface, RoundedCornerShape(16.dp))
            .border(1.dp, if (done) EcoGreen.copy(.3f) else EcoBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(quest.emoji, fontSize = 26.sp)
        Column(Modifier.weight(1f)) {
            Text(quest.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoTextPrimary)
            Spacer(Modifier.height(3.dp))
            Text(
                if (done) "Выполнено ✓" else "$progress / ${quest.target} выполнено",
                fontSize = 10.sp, color = if (done) EcoGreen else EcoTextMuted, fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = progFloat,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = if (done) EcoGreen else EcoGold, trackColor = EcoGreenDim.copy(.2f)
            )
        }
        Text("+${quest.reward} ECO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EcoGold)
    }
}

@Composable
fun ProfileSectionLabel(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, fontSize = 10.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        Box(Modifier.weight(1f).height(1.dp).background(EcoBorder))
    }
}

// ── Карточка-ссылка на другой экран ──────────────────────────────────────────

@Composable
fun ProfileNavCard(modifier: Modifier, emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .background(EcoSurface, RoundedCornerShape(16.dp))
            .border(1.dp, EcoBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoTextPrimary)
            Text(subtitle, fontSize = 10.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.weight(1f))
        Text("→", fontSize = 16.sp, color = EcoGreen, fontWeight = FontWeight.Bold)
    }
}