package com.example.ecoscanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecoscanner.ui.theme.EcoBackground
import com.example.ecoscanner.ui.theme.EcoBorder
import com.example.ecoscanner.ui.theme.EcoGold
import com.example.ecoscanner.ui.theme.EcoGreen
import com.example.ecoscanner.ui.theme.EcoGreenDim
import com.example.ecoscanner.ui.theme.EcoPurple
import com.example.ecoscanner.ui.theme.EcoSurface
import com.example.ecoscanner.ui.theme.EcoTextMuted
import com.example.ecoscanner.ui.theme.EcoTextPrimary

@Composable
fun ProfileScreen(
    onOpenEvents: () -> Unit,
    onOpenWallet: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ProfileHeroCard()
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileQuickActionCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🪙",
                    title = "Wallet",
                    subtitle = "${GameState.ecoBalance} ECO",
                    onClick = onOpenWallet
                )
                ProfileQuickActionCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🌍",
                    title = "Events",
                    subtitle = "Глобальные миссии",
                    onClick = onOpenEvents
                )
            }
        }

        item {
            SectionLabel("Статистика")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Уровень",
                    value = "${GameState.level}",
                    subtitle = GameState.levelTitle
                )
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Коллекция",
                    value = "${GameState.collection.size}",
                    subtitle = "из ${GameState.maxCollectionSlots}"
                )
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Стрик",
                    value = "${GameState.streak}",
                    subtitle = "x${"%.1f".format(GameState.streakMultiplier)}"
                )
            }
        }

        item {
            XpProgressCard()
        }

        item {
            SectionLabel("Ежедневные квесты")
        }

        items(DAILY_QUESTS) { quest ->
            ProfileQuestCard(quest)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ProfileHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = EcoSurface),
        border = BorderStroke(1.dp, EcoBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            EcoGreenDim.copy(alpha = 0.42f),
                            EcoPurple.copy(alpha = 0.18f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(EcoGreenDim.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌿", fontSize = 28.sp)
                    }

                    Column {
                        Text(
                            text = "EcoScanner Profile",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = EcoTextPrimary
                        )
                        Text(
                            text = GameState.levelTitle,
                            fontSize = 11.sp,
                            color = EcoGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Баланс",
                        fontSize = 10.sp,
                        color = EcoTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${GameState.ecoBalance} ECO",
                        fontSize = 12.sp,
                        color = EcoGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "XP",
                        fontSize = 10.sp,
                        color = EcoTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${GameState.xpForCurrent} / 1000",
                        fontSize = 11.sp,
                        color = EcoTextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = GameState.xpProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = EcoGreen,
                    trackColor = EcoGreenDim.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
fun ProfileQuickActionCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EcoSurface),
        border = BorderStroke(1.dp, EcoBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EcoTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = EcoTextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ProfileStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EcoSurface),
        border = BorderStroke(1.dp, EcoBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 9.sp,
                color = EcoTextMuted,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = EcoTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = EcoGreen,
                fontFamily = FontFamily.Monospace,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun XpProgressCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EcoSurface),
        border = BorderStroke(1.dp, EcoBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Прогресс сезона",
                fontSize = 12.sp,
                color = EcoTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ещё ${1000 - GameState.xpForCurrent} XP до следующего уровня",
                fontSize = 10.sp,
                color = EcoTextMuted,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = GameState.xpProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = EcoPurple,
                trackColor = EcoPurple.copy(alpha = 0.18f)
            )
        }
    }
}

@Composable
fun ProfileQuestCard(quest: DailyQuest) {
    val progressValue = GameState.questProgress[quest.id] ?: 0
    val done = GameState.isQuestDone(quest.id)
    val progressFloat = GameState.questProgressFloat(quest.id)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EcoSurface),
        border = BorderStroke(
            1.dp,
            if (done) EcoGreen.copy(alpha = 0.45f) else EcoBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EcoGreenDim.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(quest.emoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoTextPrimary
                    )
                    Text(
                        text = if (done) "Завершено" else "$progressValue / ${quest.target}",
                        fontSize = 10.sp,
                        color = if (done) EcoGreen else EcoTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "+${quest.reward}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoGold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = progressFloat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (done) EcoGreen else EcoGold,
                trackColor = EcoGreenDim.copy(alpha = 0.2f)
            )
        }
    }
}