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
    var lastCard by remember { mutableStateOf<EcoCard?>(null) }
    var showReward by remember { mutableStateOf(false) }
    var pendingMapObj by remember { mutableStateOf<MapObject?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBackground)
            .systemBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp),
            label = "screen"
        ) { screen ->
            when (screen) {
                "map" -> MapScreen(
                    onNavigateToScanner = { mapObj ->
                        pendingMapObj = mapObj
                        currentScreen = "scanner"
                    }
                )

                "scanner" -> ScannerScreen(
                    pendingObject = pendingMapObj,
                    onScan = { card ->
                        GameState.recordScan(card)
                        lastCard = card
                        showReward = true
                        pendingMapObj = null
                    },
                    onClearPending = { pendingMapObj = null }
                )

                "collection" -> CollectionScreen(
                    collection = GameState.collection,
                    balance = GameState.ecoBalance
                )

                "shop" -> ShopScreen()

                "profile" -> ProfileScreen(
                    onOpenEvents = { currentScreen = "events_internal" },
                    onOpenWallet = { currentScreen = "wallet_internal" }
                )

                "wallet_internal" -> WalletScreen(balance = GameState.ecoBalance)

                "events_internal" -> EventsScreen()
            }
        }

        EcoBottomNav(
            current = currentScreen,
            onSelect = {
                currentScreen = when (it) {
                    "wallet_internal" -> "profile"
                    "events_internal" -> "profile"
                    else -> it
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showReward && lastCard != null) {
            RewardOverlay(
                card = lastCard!!,
                onDismiss = { showReward = false }
            )
        }
    }
}
// ─── Bottom Navigation ────────────────────────────────────────────────────────
@Composable
fun EcoBottomNav(current: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val normalizedCurrent = when (current) {
        "wallet_internal", "events_internal" -> "profile"
        else -> current
    }

    val items = listOf(
        Triple("map", "🗺️", "Карта"),
        Triple("scanner", "📷", "Сканер"),
        Triple("collection", "🃏", "Коллекция"),
        Triple("shop", "🛒", "Магазин"),
        Triple("profile", "👤", "Профиль")
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = EcoSurface.copy(alpha = 0.98f),
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
                        end = Offset(size.width, 0f),
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
                    val selected = normalizedCurrent == key
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(icon, fontSize = 20.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                color = if (selected) EcoGreen else EcoTextMuted,
                                fontFamily = FontFamily.Monospace
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

// ─── Scanner Screen ───────────────────────────────────────────────────────────

@Composable
fun ScannerScreen(
    pendingObject: MapObject? = null,
    onScan: (EcoCard) -> Unit,
    onClearPending: () -> Unit = {}
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasCamera      = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    // КД тикер
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            tick = System.currentTimeMillis()
        }
    }

    val scannerCd  = GameState.scannerCdRemaining()
    val plantCd    = pendingObject?.let { GameState.plantCdRemaining(it.id) } ?: 0L
    val canScan    = scannerCd == 0L && plantCd == 0L

    var scanning   by remember { mutableStateOf(false) }
    val scanProgress by animateFloatAsState(
        targetValue   = if (scanning) 1f else 0f,
        animationSpec = tween(1800, easing = LinearEasing),
        finishedListener = {
            if (scanning) {
                // Скандируем конкретный объект с карты или случайный
                val card = pendingObject?.toEcoCard() ?: PLANT_DATABASE.random()
                onScan(card)
                scanning = false
            }
        }, label = "scan"
    )

    val cornerAlpha by rememberInfiniteTransition(label = "corner").animateFloat(
        initialValue  = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBackground)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (pendingObject != null) pendingObject.emoji + " " + pendingObject.name.split(" ").first()
                    else "EcoScanner",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Black,
                    color      = EcoGreen
                )
                Text(
                    text = if (pendingObject != null) pendingObject.rarity.label
                    else "AR · Сканер",
                    fontSize   = 10.sp,
                    color      = if (pendingObject != null) pendingObject.rarity.color else EcoTextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
            Row(
                modifier = Modifier
                    .background(EcoRed.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .border(1.dp, EcoRed.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).background(EcoRed, CircleShape))
                Text("LIVE", fontSize = 9.sp, color = EcoRed, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            }
        }

        // Camera box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF061406))
                .border(
                    1.dp,
                    if (pendingObject != null) pendingObject.rarity.color.copy(alpha = 0.4f) else EcoGreenDim,
                    RoundedCornerShape(24.dp)
                )
        ) {
            if (hasCamera) {
                AndroidView(
                    factory = { ctx ->
                        val pv = PreviewView(ctx)
                        ProcessCameraProvider.getInstance(ctx).addListener({
                            try {
                                val cp = ProcessCameraProvider.getInstance(ctx).get()
                                val preview = Preview.Builder().build()
                                preview.setSurfaceProvider(pv.surfaceProvider)
                                cp.unbindAll()
                                cp.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(ctx))
                        pv
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFF061406)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Разрешите доступ к камере", color = EcoTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            ScanFrame(alpha = cornerAlpha, progress = if (scanning) scanProgress else 0f)

            if (!scanning) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        if (pendingObject != null) "Наведите на ${pendingObject.name.split(" ").first()}"
                        else "Наведите на растение",
                        color = EcoTextPrimary, fontSize = 12.sp
                    )
                }
            }

            if (scanning) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .background(EcoSurface.copy(alpha = 0.93f), RoundedCornerShape(14.dp))
                        .border(1.dp, EcoBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(7.dp).background(EcoGreen, CircleShape))
                            Text("Анализ объекта...", color = EcoGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text("${(scanProgress * 100).toInt()}%", color = EcoGold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress   = scanProgress,
                        modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color      = EcoGreen,
                        trackColor = EcoGreenDim.copy(alpha = 0.3f)
                    )
                }
            }
        }

        // КД предупреждение
        if (!canScan && !scanning) {
            val (cdIcon, cdText, cdColor) = when {
                plantCd > 0   -> Triple("🔒", "Уже отсканировано · КД: ${GameState.formatCd(plantCd)}", EcoRed)
                else          -> Triple("⏳", "Сканер на перезарядке · ${GameState.formatCd(scannerCd)}", EcoGold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(cdColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(1.dp, cdColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(cdIcon, fontSize = 18.sp)
                Text(cdText, fontSize = 11.sp, color = cdColor, fontFamily = FontFamily.Monospace)
            }
        }

        // Scan button
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button(
                onClick  = { if (!scanning && canScan) scanning = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled  = !scanning && canScan,
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor        = EcoGreenDark,
                    contentColor          = EcoBackground,
                    disabledContainerColor= EcoGreenDim.copy(alpha = 0.3f),
                    disabledContentColor  = EcoTextMuted
                )
            ) {
                Text(
                    when {
                        scanning    -> "⏳ Сканирование..."
                        !canScan    -> "🔒 Недоступно"
                        else        -> "📷 Сканировать"
                    },
                    fontSize = 16.sp, fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// Scan frame with animated corners + scan line
@Composable
fun ScanFrame(alpha: Float, progress: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        val cornerSize  = 24.dp
        val strokeWidth = 2.dp
        val color       = EcoGreen.copy(alpha = alpha)

        // Corners via Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cs  = cornerSize.toPx()
            val sw  = strokeWidth.toPx()
            val pad = size.width * 0.18f
            val left   = pad
            val right  = size.width - pad
            val top    = size.height * 0.25f
            val bottom = size.height * 0.75f

            val p = androidx.compose.ui.graphics.Paint().apply {
                this.color = color
                this.strokeWidth = sw
                this.style = PaintingStyle.Stroke
            }

            // TL
            drawContext.canvas.drawLine(Offset(left, top + cs), Offset(left, top), p)
            drawContext.canvas.drawLine(Offset(left, top), Offset(left + cs, top), p)
            // TR
            drawContext.canvas.drawLine(Offset(right - cs, top), Offset(right, top), p)
            drawContext.canvas.drawLine(Offset(right, top), Offset(right, top + cs), p)
            // BL
            drawContext.canvas.drawLine(Offset(left, bottom - cs), Offset(left, bottom), p)
            drawContext.canvas.drawLine(Offset(left, bottom), Offset(left + cs, bottom), p)
            // BR
            drawContext.canvas.drawLine(Offset(right - cs, bottom), Offset(right, bottom), p)
            drawContext.canvas.drawLine(Offset(right, bottom - cs), Offset(right, bottom), p)

            // Scan line
            if (progress > 0f) {
                val scanY = top + (bottom - top) * progress
                val scanBrush = Brush.horizontalGradient(
                    listOf(Color.Transparent, EcoGreen, Color.Transparent),
                    startX = left, endX = right
                )
                drawLine(
                    brush       = scanBrush,
                    start       = Offset(left, scanY),
                    end         = Offset(right, scanY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

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

// ─── Events Screen ────────────────────────────────────────────────────────────

@Composable
fun EventsScreen() {
    val events = listOf(
        Triple("🔥", "Весеннее цветение",    "Copernicus зафиксировал массовое цветение в Карпатах. Сканируй весенние цветы — ×10 токены!"),
        Triple("🌋", "Вулканические минералы","Редкие породы Камчатки доступны 8 дней. Лимитированные карточки."),
        Triple("🌊", "Прибрежные водоросли", "Глобальное событие по всем береговым линиям. ×3 токены за морскую флору."),
    )

    LazyColumn(
        modifier        = Modifier.fillMaxSize().background(EcoBackground),
        contentPadding  = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ивенты", fontSize = 26.sp, fontWeight = FontWeight.Black, color = EcoTextPrimary)
                Row(
                    modifier = Modifier
                        .background(EcoRed.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .border(1.dp, EcoRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.size(5.dp).background(EcoRed, CircleShape))
                    Text("LIVE", fontSize = 8.sp, color = EcoRed, fontFamily = FontFamily.Monospace)
                }
            }
        }

        item { SectionLabel("Активные события") }

        items(events.take(1)) { (emoji, name, desc) ->
            EventCardFeatured(emoji, name, desc)
        }

        item { SectionLabel("Все события") }

        items(events.drop(1)) { (emoji, name, desc) ->
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .background(EcoSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, EcoBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoTextPrimary)
                    Text(desc.take(60) + "...", fontSize = 10.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace, lineHeight = 14.sp)
                }
                Text("×5", fontSize = 14.sp, fontWeight = FontWeight.Black, color = EcoGold)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun EventCardFeatured(emoji: String, name: String, desc: String) {
    var progress by remember { mutableStateOf(0.62f) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.Transparent),
        border   = BorderStroke(1.dp, EcoPurple.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(EcoPurple.copy(alpha = 0.08f), EcoGreen.copy(alpha = 0.04f)))
                )
                .padding(16.dp)
        ) {
            Column {
                Text(emoji, fontSize = 36.sp)
                Spacer(Modifier.height(6.dp))
                Text(name, fontSize = 17.sp, fontWeight = FontWeight.Black, color = EcoTextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(desc, fontSize = 11.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                Spacer(Modifier.height(12.dp))
                // Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Прогресс сообщества", fontSize = 9.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                    Text("${(progress * 100).toInt()}%", fontSize = 9.sp, color = EcoPurple, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress      = progress,
                    modifier      = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color         = EcoPurple,
                    trackColor    = EcoPurple.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(12.dp))
                // Rewards
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("🪙 ×10 ECO", "🃏 Лимит.", "🏆 Ачивка").forEach { reward ->
                        Box(
                            modifier = Modifier
                                .background(EcoGreenDim.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .border(1.dp, EcoBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(reward, fontSize = 10.sp, color = EcoTextPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = EcoPurple.copy(alpha = 0.25f),
                        contentColor   = EcoPurple
                    ),
                    border   = BorderStroke(1.dp, EcoPurple.copy(alpha = 0.5f))
                ) {
                    Text("Участвовать →", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}