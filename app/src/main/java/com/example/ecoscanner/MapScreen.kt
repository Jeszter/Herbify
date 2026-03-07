package com.example.ecoscanner

// ─── Добавить в app/build.gradle ─────────────────────────────────────────────
//   implementation("com.google.android.gms:play-services-location:21.3.0")
//   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
// ─────────────────────────────────────────────────────────────────────────────

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import com.example.ecoscanner.ui.theme.*
import kotlinx.coroutines.delay

// ─── MapScreen ────────────────────────────────────────────────────────────────

@Composable
fun MapScreen(
    onNavigateToScanner: (MapObject) -> Unit
) {
    val context = LocalContext.current

    var nearbyObjects   by remember { mutableStateOf<List<MapObject>>(MapObjectsRepository.ALL_MAP_OBJECTS) }
    var selectedObject  by remember { mutableStateOf<MapObject?>(null) }
    var filterRarity    by remember { mutableStateOf<Rarity?>(null) }
    var filterBiome     by remember { mutableStateOf<Biome?>(null) }
    var showFilters     by remember { mutableStateOf(false) }

    // Фиктивные координаты для демо (замени на реальные из FusedLocationProviderClient)
    val userLat = 50.4501
    val userLon = 30.5241

    // КД тикер
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            tick = System.currentTimeMillis()
        }
    }

    val hasLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // Фильтрованный список
    val displayedObjects: List<MapObject> = remember(nearbyObjects, filterRarity, filterBiome, tick) {
        nearbyObjects.filter { obj ->
            (filterRarity == null || obj.rarity == filterRarity) &&
                    (filterBiome  == null || obj.biome  == filterBiome)
        }
    }

    val nearestScannable: MapObject? = remember(displayedObjects, tick) {
        displayedObjects
            .filter { !it.isOnCooldown() }
            .minByOrNull { distanceM(userLat, userLon, it.lat, it.lon) }
    }

    val epicCount      = displayedObjects.count { it.rarity == Rarity.EPIC      && !it.isOnCooldown() }
    val legendaryCount = displayedObjects.count { it.rarity == Rarity.LEGENDARY && !it.isOnCooldown() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBackground)
    ) {
        MapTopBar(
            objectCount   = displayedObjects.size,
            onFilterClick = { showFilters = !showFilters }
        )

        if (showFilters) {
            MapFilters(
                selectedRarity = filterRarity,
                selectedBiome  = filterBiome,
                onRaritySelect = { r -> filterRarity = if (filterRarity == r) null else r },
                onBiomeSelect  = { b -> filterBiome  = if (filterBiome  == b) null else b }
            )
        }

        Box(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
            MapCanvas(
                objects        = displayedObjects,
                selectedId     = selectedObject?.id,
                onObjectClick  = { obj ->
                    selectedObject = if (selectedObject?.id == obj.id) null else obj
                },
                modifier       = Modifier.fillMaxSize()
            )

            MapStatsOverlay(
                total     = displayedObjects.size,
                epic      = epicCount,
                legendary = legendaryCount,
                modifier  = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }

        // Нижняя панель
        val sel = selectedObject
        if (sel != null) {
            MapObjectDetailCard(
                obj      = sel,
                userLat  = userLat,
                userLon  = userLon,
                tick     = tick,
                onScan   = { onNavigateToScanner(it) },
                onDismiss = { selectedObject = null }
            )
        } else if (nearestScannable != null) {
            MapNearbyCard(
                obj        = nearestScannable,
                userLat    = userLat,
                userLon    = userLon,
                onNavigate = { onNavigateToScanner(it) }
            )
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
fun MapTopBar(objectCount: Int, onFilterClick: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("EcoScanner", fontSize = 20.sp, fontWeight = FontWeight.Black, color = EcoGreen)
            Text(
                "$objectCount объектов · ${GameState.radarRadiusM}м радар",
                fontSize = 9.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .background(EcoSurface, RoundedCornerShape(10.dp))
                    .border(1.dp, EcoBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onFilterClick)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text("⚙️ Фильтр", fontSize = 10.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
            }
            Row(
                modifier = Modifier
                    .background(EcoRed.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .border(1.dp, EcoRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(Modifier.size(6.dp).background(EcoRed, CircleShape))
                Text("LIVE", fontSize = 9.sp, color = EcoRed, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ─── Filters ──────────────────────────────────────────────────────────────────

@Composable
fun MapFilters(
    selectedRarity: Rarity?,
    selectedBiome: Biome?,
    onRaritySelect: (Rarity) -> Unit,
    onBiomeSelect: (Biome) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EcoSurface)
            .border(1.dp, EcoBorder)
            .padding(10.dp)
    ) {
        Text("РЕДКОСТЬ", fontSize = 8.sp, color = EcoTextMuted,
            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(Rarity.values().toList()) { rarity ->
                val selected = selectedRarity == rarity
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) rarity.color.copy(alpha = 0.2f) else EcoBackground)
                        .border(1.dp, if (selected) rarity.color else EcoBorder, RoundedCornerShape(8.dp))
                        .clickable { onRaritySelect(rarity) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(rarity.label, fontSize = 9.sp,
                        color = if (selected) rarity.color else EcoTextMuted,
                        fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("БИОМ", fontSize = 8.sp, color = EcoTextMuted,
            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(Biome.values().toList()) { biome ->
                val selected = selectedBiome == biome
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) EcoGreenDim else EcoBackground)
                        .border(1.dp, if (selected) EcoGreen else EcoBorder, RoundedCornerShape(8.dp))
                        .clickable { onBiomeSelect(biome) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("${biome.emoji} ${biome.label}", fontSize = 9.sp,
                        color = if (selected) EcoGreen else EcoTextMuted,
                        fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// ─── Map Canvas (заглушка — замени на MapboxMap или GoogleMap) ────────────────

@Composable
fun MapCanvas(
    objects: List<MapObject>,
    selectedId: Int?,
    onObjectClick: (MapObject) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "map")
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.8f,
        targetValue   = 0.1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label         = "ping"
    )

    BoxWithConstraints(modifier = modifier.background(Color(0xFF080D08))) {
        val canvasW = maxWidth
        val canvasH = maxHeight

        // Фоновая сетка
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 30.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(Color(0xFF1A3D24).copy(alpha = 0.4f), Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(Color(0xFF1A3D24).copy(alpha = 0.4f), Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }

        // Маркеры объектов — равномерно по экрану
        objects.forEachIndexed { index, obj ->
            val col  = index % 3
            val row  = index / 3
            val offX = canvasW * (0.15f + col * 0.30f)
            val offY = canvasH * (0.15f + row * 0.20f)

            Box(
                modifier = Modifier
                    .offset(x = offX - 20.dp, y = offY - 20.dp)
                    .size(40.dp)
                    .clickable { onObjectClick(obj) },
                contentAlignment = Alignment.Center
            ) {
                val isSelected   = selectedId == obj.id
                val onCooldown   = obj.isOnCooldown()
                val markerColor  = if (onCooldown) EcoTextMuted else obj.rarity.color

                // Пульсация Legendary
                if (obj.rarity == Rarity.LEGENDARY && !onCooldown) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(markerColor.copy(alpha = pingAlpha * 0.15f), CircleShape)
                            .border(1.dp, markerColor.copy(alpha = pingAlpha * 0.4f), CircleShape)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            markerColor.copy(alpha = if (isSelected) 0.3f else 0.15f),
                            CircleShape
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (onCooldown) markerColor.copy(alpha = 0.4f) else markerColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(obj.emoji, fontSize = if (onCooldown) 14.sp else 16.sp)
                }

                // КД значок
                if (onCooldown) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp)
                            .background(EcoRed.copy(alpha = 0.9f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("⏳", fontSize = 7.sp) }
                }
            }
        }

        // Маркер игрока (центр)
        Box(
            modifier = Modifier
                .offset(x = canvasW / 2 - 20.dp, y = canvasH / 2 - 20.dp)
                .size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(60.dp).background(EcoBlue.copy(alpha = pingAlpha * 0.15f), CircleShape))
            Box(Modifier.size(36.dp).background(EcoBlue.copy(alpha = pingAlpha * 0.1f), CircleShape))
            Box(Modifier.size(12.dp).background(EcoBlue, CircleShape).border(2.dp, Color.White, CircleShape))
        }
    }
}

// ─── Stats Overlay ────────────────────────────────────────────────────────────

@Composable
fun MapStatsOverlay(total: Int, epic: Int, legendary: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(EcoSurface.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
            .border(1.dp, EcoBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text("$total", fontSize = 16.sp, fontWeight = FontWeight.Black, color = EcoGreen)
        Text("объектов", fontSize = 7.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
        if (epic > 0)
            Text("$epic epic", fontSize = 8.sp, color = EcoPurple, fontFamily = FontFamily.Monospace)
        if (legendary > 0)
            Text("$legendary ★", fontSize = 8.sp, color = EcoGold, fontFamily = FontFamily.Monospace)
    }
}

// ─── Nearby Card ──────────────────────────────────────────────────────────────

@Composable
fun MapNearbyCard(
    obj: MapObject,
    userLat: Double,
    userLon: Double,
    onNavigate: (MapObject) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EcoSurface)
            .border(1.dp, EcoBorder)
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(obj.emoji, fontSize = 28.sp)
        Column(Modifier.weight(1f)) {
            Text(obj.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EcoTextPrimary)
            Text(
                "📍 ${MapObjectsRepository.formatDistance(userLat, userLon, obj)} · ${obj.biome.label}",
                fontSize = 9.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .background(obj.rarity.color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .border(1.dp, obj.rarity.color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(obj.rarity.label, fontSize = 8.sp, color = obj.rarity.color,
                    fontFamily = FontFamily.Monospace)
            }
            Button(
                onClick        = { onNavigate(obj) },
                modifier       = Modifier.height(28.dp),
                shape          = RoundedCornerShape(8.dp),
                colors         = ButtonDefaults.buttonColors(
                    containerColor = EcoGreenDark,
                    contentColor   = EcoBackground
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("→ Сканировать", fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// ─── Detail Card (при тапе на маркер) ────────────────────────────────────────

@Composable
fun MapObjectDetailCard(
    obj: MapObject,
    userLat: Double,
    userLon: Double,
    tick: Long,
    onScan: (MapObject) -> Unit,
    onDismiss: () -> Unit
) {
    val inRange       = obj.isInRange(userLat, userLon)
    val onCooldown    = obj.isOnCooldown()
    val cdRemaining   = GameState.plantCdRemaining(obj.id)
    val distStr       = MapObjectsRepository.formatDistance(userLat, userLon, obj)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EcoSurface)
            .border(1.dp, obj.rarity.color.copy(alpha = 0.3f))
            .padding(14.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(obj.emoji, fontSize = 30.sp)
                Column {
                    Text(obj.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoTextPrimary)
                    Text("${obj.rarity.label} · ${obj.biome.emoji} ${obj.biome.label}",
                        fontSize = 9.sp, color = obj.rarity.color, fontFamily = FontFamily.Monospace)
                }
            }
            Text("✕", modifier = Modifier.clickable(onClick = onDismiss),
                fontSize = 18.sp, color = EcoTextMuted)
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📍", fontSize = 14.sp)
                Text(distStr, fontSize = 11.sp,
                    color = if (inRange) EcoGreen else EcoTextMuted,
                    fontFamily = FontFamily.Monospace)
                if (inRange) {
                    Box(
                        modifier = Modifier
                            .background(EcoGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .border(1.dp, EcoGreen.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text("В зоне", fontSize = 8.sp, color = EcoGreen, fontFamily = FontFamily.Monospace) }
                }
            }
            Text("+${obj.rarity.multiplier} ECO",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EcoGold)
        }

        if (onCooldown) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EcoRed.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .border(1.dp, EcoRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("🔒", fontSize = 14.sp)
                Text("Уже отсканировано · КД: ${GameState.formatCd(cdRemaining)}",
                    fontSize = 10.sp, color = EcoRed, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick  = { if (!onCooldown && inRange) onScan(obj) },
            modifier = Modifier.fillMaxWidth().height(46.dp),
            enabled  = !onCooldown && inRange,
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = EcoGreenDark,
                contentColor           = EcoBackground,
                disabledContainerColor = EcoGreenDim.copy(alpha = 0.3f),
                disabledContentColor   = EcoTextMuted
            )
        ) {
            Text(
                when {
                    onCooldown -> "🔒 Cooldown"
                    !inRange   -> "📍 Подойди ближе (${obj.scanRadiusM}м)"
                    else       -> "📷 Сканировать"
                },
                fontSize = 13.sp, fontWeight = FontWeight.Black
            )
        }
    }
}