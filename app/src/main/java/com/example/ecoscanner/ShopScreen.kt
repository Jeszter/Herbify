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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecoscanner.ui.theme.*

@Composable
fun ShopScreen() {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Апгрейды", "Бустеры", "Скины")

    Column(Modifier.fillMaxSize().background(EcoBackground)) {
        // ── Шапка ──────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🛒", fontSize = 22.sp)
                Text("Магазин", fontSize = 24.sp, fontWeight = FontWeight.Black, color = EcoTextPrimary)
            }
            Box(
                Modifier.background(EcoGold.copy(.12f), RoundedCornerShape(12.dp))
                    .border(1.dp, EcoGold.copy(.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("🪙", fontSize = 14.sp)
                    Text("${GameState.ecoBalance} ECO", fontSize = 14.sp,
                        fontWeight = FontWeight.Black, color = EcoGold)
                }
            }
        }

        // ── Табы ──────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { i, label ->
                val sel = tab == i
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (sel) EcoGreen else EcoSurface)
                        .border(1.dp, if (sel) EcoGreen else EcoBorder, RoundedCornerShape(10.dp))
                        .clickable { tab = i }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(label, fontSize = 12.sp,
                        color = if (sel) EcoBackground else EcoTextMuted,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        when (tab) {
            0 -> UpgradesTab()
            1 -> BoostersTab()
            2 -> SkinsTab()
        }
    }
}

@Composable
fun UpgradesTab() {
    val grouped = SHOP_UPGRADES.groupBy { it.category }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (category, upgrades) ->
            item {
                ProfileSectionLabel(category.label.uppercase())
                Spacer(Modifier.height(8.dp))
            }
            items(upgrades) { upg -> UpgradeRow(upg) }
            item { Spacer(Modifier.height(8.dp)) }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun UpgradeRow(upgrade: UpgradeItem) {
    var currentLevel by remember { mutableIntStateOf(GameState.upgradeLevels[upgrade.id] ?: 0) }
    val isMaxed   = currentLevel >= upgrade.maxLevel
    val nextLevel = if (!isMaxed) upgrade.levels[currentLevel] else null
    val canAfford = nextLevel != null && GameState.ecoBalance >= nextLevel.cost

    // Тег в правом верхнем углу
    val tagText = when {
        isMaxed -> "✓ Макс."
        upgrade.id == "scanner_cd" -> "−${(nextLevel?.effectValue ?: 0) / 1000}с КД"
        upgrade.id == "ai_accuracy" -> "+${nextLevel?.effectValue ?: 0}% точн."
        upgrade.id == "backpack"   -> "+${nextLevel?.effectValue ?: 0} слотов"
        upgrade.id == "radar"      -> "${200 + (nextLevel?.effectValue ?: 0).toInt()}м радиус"
        else -> "+${nextLevel?.effectValue ?: 0}%"
    }
    val tagColor = when {
        isMaxed      -> EcoGreen
        !canAfford   -> EcoRed
        else         -> EcoGreen
    }

    Row(
        Modifier.fillMaxWidth()
            .background(EcoSurface, RoundedCornerShape(16.dp))
            .border(1.dp, if (isMaxed) EcoGold.copy(.35f) else EcoBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Иконка
        Box(
            Modifier.size(52.dp).background(EcoGreenDim.copy(.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Text(upgrade.emoji, fontSize = 24.sp) }

        // Текст
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(upgrade.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoTextPrimary)
            Text(
                nextLevel?.description ?: upgrade.levels.last().description,
                fontSize = 10.sp, color = EcoTextMuted, lineHeight = 14.sp
            )
            Spacer(Modifier.height(2.dp))
            // Точки уровня
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(upgrade.maxLevel) { i ->
                    Box(
                        Modifier.size(7.dp).background(
                            if (i < currentLevel) EcoGreen else EcoGreenDim.copy(.25f), CircleShape
                        )
                    )
                }
                Text(" ${currentLevel}/${upgrade.maxLevel}", fontSize = 9.sp,
                    color = EcoTextMuted, fontFamily = FontFamily.Monospace)
            }
        }

        // Правая сторона
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Тег эффекта
            Box(
                Modifier.background(tagColor.copy(.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, tagColor.copy(.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) { Text(tagText, fontSize = 9.sp, color = tagColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }

            if (!isMaxed) {
                // Цена
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🪙", fontSize = 12.sp)
                    Text("${nextLevel!!.cost}", fontSize = 13.sp, color = EcoGold, fontWeight = FontWeight.Bold)
                }
                // Кнопка
                Button(
                    onClick = {
                        if (GameState.buyUpgrade(upgrade.id))
                            currentLevel = GameState.upgradeLevels[upgrade.id] ?: currentLevel
                    },
                    enabled = canAfford,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EcoGreenDark, contentColor = EcoBackground,
                        disabledContainerColor = EcoRed.copy(.15f), disabledContentColor = EcoRed
                    )
                ) {
                    Text(
                        if (canAfford) "Купить" else "Мало ECO",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BoostersTab() {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ProfileSectionLabel("АКТИВНЫЕ БУСТЕРЫ")
            Spacer(Modifier.height(8.dp))
            val active = SHOP_BOOSTERS.filter { GameState.isBoosterActive(it.id) }
            if (active.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().background(EcoSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, EcoBorder, RoundedCornerShape(14.dp)).padding(16.dp)
                ) { Text("Сейчас нет активных бустеров", fontSize = 11.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace) }
            }
            Spacer(Modifier.height(14.dp))
            ProfileSectionLabel("ПОКУПКА БУСТЕРОВ")
            Spacer(Modifier.height(8.dp))
        }
        items(SHOP_BOOSTERS) { b -> BoosterRow(b) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun BoosterRow(booster: BoosterItem) {
    val canAfford = GameState.ecoBalance >= booster.cost
    Row(
        Modifier.fillMaxWidth().background(EcoSurface, RoundedCornerShape(16.dp))
            .border(1.dp, EcoBorder, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(52.dp).background(EcoGreenDim.copy(.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) { Text(booster.emoji, fontSize = 24.sp) }
        Column(Modifier.weight(1f)) {
            Text(booster.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoTextPrimary)
            Text(booster.description, fontSize = 10.sp, color = EcoTextMuted, lineHeight = 14.sp)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🪙", fontSize = 12.sp)
                Text("${booster.cost}", fontSize = 13.sp, color = EcoGold, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { GameState.buyBooster(booster) },
                enabled = canAfford,
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EcoGreenDark, contentColor = EcoBackground,
                    disabledContainerColor = EcoSurface, disabledContentColor = EcoTextMuted
                )
            ) { Text("Купить", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun SkinsTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎨", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text("Скины — скоро!", fontSize = 16.sp, color = EcoTextMuted)
        }
    }
}