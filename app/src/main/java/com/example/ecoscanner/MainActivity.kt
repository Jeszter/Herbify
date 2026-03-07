package com.example.ecoscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ecoscanner.ui.theme.*
import java.util.concurrent.Executors

// ─── Data ────────────────────────────────────────────────────────────────────

enum class Rarity(val label: String, val color: Color, val multiplier: Int) {
    COMMON("Common", Color(0xFF8ECC5E), 1),
    RARE("Rare", EcoBlue, 3),
    EPIC("Epic", EcoPurple, 5),
    LEGENDARY("Legendary", EcoGold, 10)
}

data class EcoCard(
    val id: Int,
    val emoji: String,
    val name: String,
    val latin: String,
    val rarity: Rarity,
    val description: String,
    val area: Int = (60..99).random()
)

val PLANT_DATABASE = listOf(
    EcoCard(1,  "🌿", "Папоротник орляк",   "Pteridium aquilinum",  Rarity.RARE,      "Один из самых распространённых папоротников. Образует плотные заросли в хвойных лесах."),
    EcoCard(2,  "🌸", "Цикорий обыкновенный","Cichorium intybus",   Rarity.COMMON,    "Синие цветы вдоль дорог. Корень используют как заменитель кофе."),
    EcoCard(3,  "🍄", "Мухомор красный",     "Amanita muscaria",    Rarity.LEGENDARY, "Ядовитый гриб с красной шляпкой. Легендарный символ леса."),
    EcoCard(4,  "🌲", "Ель обыкновенная",    "Picea abies",         Rarity.COMMON,    "Хвойное дерево, основа таёжных лесов Европы."),
    EcoCard(5,  "🌺", "Прострел луговой",    "Pulsatilla pratensis",Rarity.EPIC,      "Редкий весенний цветок. Занесён в Красную книгу."),
    EcoCard(6,  "🪨", "Гранит",              "Granite",             Rarity.RARE,      "Самая твёрдая магматическая порода. Возраст до 3 млрд лет."),
    EcoCard(7,  "🌻", "Подсолнух",           "Helianthus annuus",   Rarity.COMMON,    "Следует за солнцем. Семена богаты витамином E."),
    EcoCard(8,  "🎋", "Бамбук обыкновенный", "Phyllostachys edulis",Rarity.EPIC,      "Самое быстрорастущее растение — до 90 см в сутки."),
    EcoCard(9,  "🌹", "Шиповник майский",    "Rosa majalis",        Rarity.RARE,      "Плоды содержат витамина C в 50 раз больше, чем лимон."),
    EcoCard(10, "🍀", "Клевер луговой",      "Trifolium pratense",  Rarity.COMMON,    "Фиксирует азот из воздуха, обогащая почву.")
)

// ─── Activity ────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.CAMERA)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (needed.isNotEmpty()) requestPermissionsLauncher.launch(needed.toTypedArray())

        setContent { MyApplicationTheme { EcoScannerApp() } }
    }
}

// ─── Root App ─────────────────────────────────────────────────────────────────

@Composable
fun EcoScannerApp() {
    var currentScreen by remember { mutableStateOf("map") }
    var lastCard      by remember { mutableStateOf<EcoCard?>(null) }
    var showReward    by remember { mutableStateOf(false) }
    var pendingMapObj by remember { mutableStateOf<MapObject?>(null) }
    var useCamera     by remember { mutableStateOf(true) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(EcoBackground)
        .systemBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            modifier = Modifier.fillMaxSize().padding(bottom = 72.dp)
        ) { screen ->
            when (screen) {
                "map" -> MapScreen(
                    onNavigateToScanner = { mapObj, withCamera ->
                        pendingMapObj = mapObj
                        useCamera     = withCamera
                        currentScreen = "scanner"
                    }
                )
                "scanner" -> ScannerScreen(
                    pendingObject  = pendingMapObj,
                    useCamera      = useCamera,
                    onScan         = { card ->
                        lastCard  = card
                        showReward = true
                        pendingMapObj = null
                    },
                    onClearPending = { pendingMapObj = null }
                )
                "collection" -> CollectionScreen(GameState.collection, GameState.ecoBalance)
                "wallet"     -> WalletScreen(GameState.ecoBalance)
                "events"     -> EventsScreen()
                "shop"       -> ShopScreen()
                "profile"    -> ProfileScreen(
                    onOpenEvents = { currentScreen = "events" },
                    onOpenWallet = { currentScreen = "wallet" }
                )
            }
        }

        EcoBottomNav(
            current  = currentScreen,
            onSelect = { currentScreen = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showReward && lastCard != null) {
            RewardOverlay(card = lastCard!!, onDismiss = { showReward = false })
        }
    }
}

// ─── Bottom Navigation ────────────────────────────────────────────────────────

@Composable
fun EcoBottomNav(current: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Triple("map",        "🗺️", "Карта"),
        Triple("scanner",    "📷", "Сканер"),
        Triple("collection", "🃏", "Карты"),
        Triple("shop",       "🛒", "Магазин"),
        Triple("profile",    "👤", "Профиль"),
    )
    Surface(
        modifier  = modifier.fillMaxWidth(),
        color     = EcoSurface.copy(alpha = 0.97f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = EcoBorder,
                        start = Offset(0f, 0f),
                        end   = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (key, icon, label) ->
                    val selected = current == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) EcoGreenDim.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(icon, fontSize = 20.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                label,
                                fontSize  = 9.sp,
                                color     = if (selected) EcoGreen else EcoTextMuted,
                                fontFamily= FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Section Label ────────────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier          = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = text.uppercase(),
            fontSize   = 9.sp,
            color      = EcoGreen,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(EcoGreenDim, Color.Transparent))
                )
        )
    }
}


// ScannerScreen вынесен в ScannerScreen.kt

// ─── Reward Overlay ───────────────────────────────────────────────────────────

@Composable
fun RewardOverlay(card: EcoCard, onDismiss: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .scale(scale)
                .clickable(enabled = false) { },
            shape  = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = EcoSurface),
            border = BorderStroke(1.dp, card.rarity.color.copy(alpha = 0.6f))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Rarity bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(listOf(card.rarity.color, EcoBlue))
                        )
                )
                // Card image area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(card.rarity.color.copy(alpha = 0.12f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(card.emoji, fontSize = 72.sp)
                    // Rarity badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(card.rarity.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(1.dp, card.rarity.color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("★ ${card.rarity.label.uppercase()}", fontSize = 8.sp, color = card.rarity.color, fontFamily = FontFamily.Monospace)
                    }
                }
                // Info
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(card.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = EcoTextPrimary)
                    Text(card.latin, fontSize = 10.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    Text(card.description, fontSize = 12.sp, color = EcoTextMuted, textAlign = TextAlign.Center, lineHeight = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    // Token reward
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EcoGold.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, EcoGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪙", fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("+${card.rarity.multiplier} ECO", fontSize = 18.sp, fontWeight = FontWeight.Black, color = EcoGold)
                            Text("${card.rarity.label} · ×${card.rarity.multiplier} бонус", fontSize = 9.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoGreenDark, contentColor = EcoBackground)
                    ) {
                        Text("Добавить в коллекцию", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ─── Collection Screen ────────────────────────────────────────────────────────

@Composable
fun CollectionScreen(collection: List<EcoCard>, balance: Int) {
    val rarities = Rarity.values()
    var filter by remember { mutableStateOf<Rarity?>(null) }
    val filtered = if (filter == null) collection else collection.filter { it.rarity == filter }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Коллекция", fontSize = 26.sp, fontWeight = FontWeight.Black, color = EcoTextPrimary)
        Text("${collection.size} / ${PLANT_DATABASE.size} карточек", fontSize = 10.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(12.dp))

        // Stats row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val statsData = listOf(
                Triple(collection.size.toString(), "Собрано", EcoGreen),
                Triple(collection.count { it.rarity == Rarity.RARE || it.rarity == Rarity.EPIC }.toString(), "Редких", EcoBlue),
                Triple(collection.count { it.rarity == Rarity.LEGENDARY }.toString(), "Легенд.", EcoGold),
                Triple("$balance", "ECO", EcoGold)
            )
            statsData.forEach { (value, label, color) ->
                Card(
                    modifier = Modifier.weight(1f),
                    colors   = CardDefaults.cardColors(containerColor = EcoSurface),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, EcoBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
                        Text(label, fontSize = 8.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Filter tabs
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(filter == null, "Все") { filter = null }
            }
            items(rarities) { r ->
                FilterChip(filter == r, r.label) { filter = if (filter == r) null else r }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Cards grid
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered.chunked(3)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { card ->
                        MiniEcoCard(card, Modifier.weight(1f))
                    }
                    // Fill empty slots
                    repeat(3 - row.size) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(0.67f)
                            .background(EcoSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, EcoBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("🔒", fontSize = 20.sp, color = EcoTextMuted) }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun FilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) EcoGreenDim.copy(alpha = 0.7f) else EcoSurface)
            .border(1.dp, if (selected) EcoGreen else EcoBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 10.sp, color = if (selected) EcoGreen else EcoTextMuted, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun MiniEcoCard(card: EcoCard, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(0.67f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF0E1E0A), Color(0xFF162608)))
            )
            .border(1.dp, card.rarity.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(card.emoji, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(card.name.split(" ").first(), fontSize = 8.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
        }
        // Rarity dot
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(8.dp)
                .background(card.rarity.color, CircleShape)
        )
    }
}

// ─── Wallet Screen ────────────────────────────────────────────────────────────

data class TxItem(val icon: String, val name: String, val amount: String, val isPlus: Boolean)

@Composable
fun WalletScreen(balance: Int) {
    val transactions = listOf(
        TxItem("🌿", "Папоротник орляк",    "+3 ECO",  true),
        TxItem("🌍", "Ивент-бонус",          "+10 ECO", true),
        TxItem("🃏", "Обмен карточки",       "-5 ECO",  false),
        TxItem("🍄", "Мухомор (легенд.)",    "+10 ECO", true),
        TxItem("🌸", "Цикорий обыкновенный", "+1 ECO",  true),
        TxItem("🪨", "Гранит Rare",          "+3 ECO",  true),
    )

    LazyColumn(
        modifier          = Modifier.fillMaxSize().background(EcoBackground),
        contentPadding    = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        item { Text("Кошелёк", fontSize = 26.sp, fontWeight = FontWeight.Black, color = EcoTextPrimary) }

        // Balance card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = Color.Transparent),
                border   = BorderStroke(1.dp, EcoGreen.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF1A2E0A), Color(0xFF0E1E06)))
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text("POLYGON NETWORK", fontSize = 9.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$balance", fontSize = 36.sp, fontWeight = FontWeight.Black, color = EcoGreen)
                            Spacer(Modifier.width(6.dp))
                            Text("ECO", fontSize = 16.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text("≈ ${"%.2f".format(balance * 0.052)} USD", fontSize = 12.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0x4a2f...c9d1", fontSize = 10.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                            Text("📋", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Action buttons
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("📤" to "Вывести", "🔄" to "Обменять", "🛒" to "В игре", "📊" to "История").forEach { (icon, label) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(containerColor = EcoSurface),
                        border   = BorderStroke(1.dp, EcoBorder)
                    ) {
                        Column(
                            modifier            = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(icon, fontSize = 20.sp)
                            Spacer(Modifier.height(3.dp))
                            Text(label, fontSize = 8.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Transactions header
        item { SectionLabel("Транзакции") }

        // Transactions
        items(transactions) { tx ->
            val (icon, name, amount, isPlus) = tx
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .background(EcoSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, EcoBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier        = Modifier
                        .size(36.dp)
                        .background(EcoGreenDim.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(icon, fontSize = 18.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, fontSize = 13.sp, color = EcoTextPrimary)
                    Text("Сегодня", fontSize = 9.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                }
                Text(amount, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (isPlus) EcoGreen else EcoRed)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}