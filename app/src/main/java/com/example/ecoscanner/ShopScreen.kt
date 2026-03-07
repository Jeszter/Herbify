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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecoscanner.ui.theme.EcoBackground
import com.example.ecoscanner.ui.theme.EcoBorder
import com.example.ecoscanner.ui.theme.EcoGold
import com.example.ecoscanner.ui.theme.EcoGreen
import com.example.ecoscanner.ui.theme.EcoGreenDark
import com.example.ecoscanner.ui.theme.EcoGreenDim
import com.example.ecoscanner.ui.theme.EcoRed
import com.example.ecoscanner.ui.theme.EcoSurface
import com.example.ecoscanner.ui.theme.EcoTextMuted
import com.example.ecoscanner.ui.theme.EcoTextPrimary

@Composable
fun ShopScreen() {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("⚡ Апгрейды", "🧪 Бустеры")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Магазин",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = EcoTextPrimary
                )
                Text(
                    text = "Улучшай сканер и усиливай награды",
                    fontSize = 10.sp,
                    color = EcoTextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .background(EcoGold.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .border(1.dp, EcoGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🪙", fontSize = 16.sp)
                    Text(
                        text = "${GameState.ecoBalance}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = EcoGold
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tabs.size) { index ->
                val selected = tab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) EcoGreenDim.copy(alpha = 0.7f) else EcoSurface)
                        .border(1.dp, if (selected) EcoGreen else EcoBorder, RoundedCornerShape(10.dp))
                        .clickable { tab = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tabs[index],
                        fontSize = 10.sp,
                        color = if (selected) EcoGreen else EcoTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (tab) {
            0 -> UpgradesTab()
            1 -> BoostersTab()
        }
    }
}

@Composable
fun UpgradesTab() {
    val grouped = SHOP_UPGRADES.groupBy { it.category }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        grouped.forEach { (category, upgrades) ->
            item {
                SectionLabel(category.label)
                Spacer(modifier = Modifier.height(6.dp))
            }

            items(upgrades) { upgrade ->
                UpgradeCard(upgrade)
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun UpgradeCard(upgrade: UpgradeItem) {
    var currentLevel by remember { mutableIntStateOf(GameState.upgradeLevels[upgrade.id] ?: 0) }
    val isMaxed = currentLevel >= upgrade.maxLevel
    val nextLevel = if (!isMaxed) upgrade.levels[currentLevel] else null
    val canAfford = nextLevel != null && GameState.ecoBalance >= nextLevel.cost

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EcoSurface),
        border = BorderStroke(1.dp, if (isMaxed) EcoGold.copy(alpha = 0.4f) else EcoBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(EcoGreenDim.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(upgrade.emoji, fontSize = 20.sp)
                    }

                    Column {
                        Text(
                            text = upgrade.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoTextPrimary
                        )
                        Text(
                            text = upgrade.category.label,
                            fontSize = 9.sp,
                            color = EcoTextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(upgrade.maxLevel) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (index < currentLevel) EcoGreen else EcoGreenDim.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isMaxed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EcoGold.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
                        .border(1.dp, EcoGold.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = upgrade.levels.last().description,
                        fontSize = 10.sp,
                        color = EcoGold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EcoBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, EcoBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "Следующий уровень",
                            fontSize = 9.sp,
                            color = EcoTextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nextLevel!!.description,
                            fontSize = 11.sp,
                            color = EcoTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = currentLevel.toFloat() / upgrade.maxLevel.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isMaxed) EcoGold else EcoGreen,
                trackColor = EcoGreenDim.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (GameState.buyUpgrade(upgrade.id)) {
                        currentLevel = GameState.upgradeLevels[upgrade.id] ?: currentLevel
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isMaxed && canAfford,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EcoGreenDark,
                    contentColor = EcoBackground,
                    disabledContainerColor = EcoSurface,
                    disabledContentColor = EcoTextMuted
                )
            ) {
                Text(
                    text = when {
                        isMaxed -> "Максимальный уровень"
                        canAfford -> "🪙 ${nextLevel!!.cost} ECO — Купить"
                        else -> "🪙 ${nextLevel!!.cost} ECO — Недостаточно"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BoostersTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionLabel("Активные бустеры")
        }

        item {
            ActiveBoostersCard()
        }

        item {
            SectionLabel("Покупка бустеров")
        }

        items(SHOP_BOOSTERS) { booster ->
            BoosterCard(booster)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ActiveBoostersCard() {
    val active = listOf(
        "cd_half" to "Ускорение сканера",
        "double_eco" to "Двойные ECO",
        "rare_boost" to "Шанс на редкость"
    ).filter { GameState.isBoosterActive(it.first) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EcoSurface),
        border = BorderStroke(1.dp, EcoBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (active.isEmpty()) {
                Text(
                    text = "Сейчас нет активных бустеров",
                    fontSize = 11.sp,
                    color = EcoTextMuted,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                active.forEach { (_, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            color = EcoTextPrimary
                        )
                        Text(
                            text = "ACTIVE",
                            fontSize = 10.sp,
                            color = EcoGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun BoosterCard(booster: BoosterItem) {
    val canAfford = GameState.ecoBalance >= booster.cost

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EcoSurface),
        border = BorderStroke(1.dp, EcoBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(EcoGreenDim.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(booster.emoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booster.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoTextPrimary
                    )
                    Text(
                        text = booster.description,
                        fontSize = 10.sp,
                        color = EcoTextMuted,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { GameState.buyBooster(booster) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canAfford,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EcoGreenDark,
                    contentColor = EcoBackground,
                    disabledContainerColor = EcoSurface,
                    disabledContentColor = EcoRed
                )
            ) {
                Text(
                    text = if (canAfford) "🪙 ${booster.cost} ECO — Купить" else "🪙 ${booster.cost} ECO — Недостаточно",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}