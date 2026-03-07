package com.example.ecoscanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ecoscanner.ui.theme.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// ─── CartoDB Dark tile source ─────────────────────────────────────────────────

fun darkMapTileSource() = object : XYTileSource(
    "CartoDB_DarkMatter", 0, 19, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/"
    )
) {
    override fun getTileURLString(pTileIndex: Long) =
        baseUrl + MapTileIndex.getZoom(pTileIndex) + "/" +
                MapTileIndex.getX(pTileIndex) + "/" +
                MapTileIndex.getY(pTileIndex) + mImageFilenameEnding
}

// ─── GeoPoint → screen pixels ────────────────────────────────────────────────

fun MapView.geoToScreen(lat: Double, lon: Double): Offset? {
    return try {
        val pt = projection?.toPixels(GeoPoint(lat, lon), null) ?: return null
        Offset(pt.x.toFloat(), pt.y.toFloat())
    } catch (_: Exception) { null }
}

// ─── Map Screen ───────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(onNavigateToScanner: (MapObject, Boolean) -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val hasLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    var userLat        by remember { mutableStateOf(50.4501) }
    var userLng        by remember { mutableStateOf(30.5234) }
    var selectedObject by remember { mutableStateOf<MapObject?>(null) }
    var showScanChoice by remember { mutableStateOf(false) }
    var mapViewRef     by remember { mutableStateOf<MapView?>(null) }

    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(100); tick++ } }

    var cdTick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(1000); cdTick = System.currentTimeMillis() } }

    val ecoObjects = MapObjectsRepository.ALL_MAP_OBJECTS

    // Biome spawn
    LaunchedEffect(userLat, userLng) {
        if (userLat == 50.4501 && userLng == 30.5234) return@LaunchedEffect
        val biomes  = BiomeSpawner.fetchBiomesNear(userLat, userLng)
        val spawned = BiomeSpawner.spawnObjects(userLat, userLng, biomes, count = 14)
        MapObjectsRepository.updateDynamic(spawned)
    }

    // GPS
    LaunchedEffect(hasLocation) {
        if (!hasLocation) return@LaunchedEffect
        try {
            LocationServices.getFusedLocationProviderClient(context)
                .lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        userLat = loc.latitude
                        userLng = loc.longitude
                        mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                    }
                }
        } catch (_: Exception) {}
    }

    // Lifecycle
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE  -> mapViewRef?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Box(Modifier.fillMaxSize()) {

        // ── OSMDroid MapView ──────────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(darkMapTileSource())
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(userLat, userLng))
                    zoomController.setVisibility(
                        org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                    )
                    if (hasLocation) {
                        val locOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locOverlay.enableMyLocation()
                        val pb = android.graphics.Bitmap.createBitmap(40, 40, android.graphics.Bitmap.Config.ARGB_8888)
                        val pc = android.graphics.Canvas(pb)
                        pc.drawCircle(20f, 20f, 18f, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(50, 0, 200, 255)
                        })
                        pc.drawCircle(20f, 20f, 18f, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(180, 0, 200, 255)
                            style = android.graphics.Paint.Style.STROKE; strokeWidth = 3f
                        })
                        pc.drawCircle(20f, 20f, 8f, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(255, 0, 230, 255)
                        })
                        locOverlay.setPersonIcon(pb)
                        locOverlay.setDirectionArrow(pb, pb)
                        overlays.add(locOverlay)
                    }
                    mapViewRef = this
                }
            }
        )

        // ── Radar grid + markers via Compose Canvas ───────────────────────
        val mv = mapViewRef
        val tickVal = tick

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(ecoObjects) {
                    detectTapGestures { tapOffset ->
                        val mapView = mapViewRef ?: return@detectTapGestures
                        val hitRadius = 44.dp.toPx() / 2
                        val hit = ecoObjects.firstOrNull { obj ->
                            val scr = mapView.geoToScreen(obj.lat, obj.lon) ?: return@firstOrNull false
                            (tapOffset - scr).getDistance() < hitRadius
                        }
                        selectedObject = if (hit != null && selectedObject?.id == hit.id) null else hit
                    }
                }
        ) {
            // Grid
            val gridColor = Color(0x0A3DFF6E)
            var x = 0f
            while (x < size.width)  { drawLine(gridColor, Offset(x,0f), Offset(x,size.height), 1f); x += 80f }
            var y = 0f
            while (y < size.height) { drawLine(gridColor, Offset(0f,y), Offset(size.width,y), 1f); y += 80f }

            // Markers
            if (mv != null) {
                ecoObjects.forEach { obj ->
                    val scr = mv.geoToScreen(obj.lat, obj.lon) ?: return@forEach
                    val isSelected = selectedObject?.id == obj.id
                    val r   = if (isSelected) 28f else 22f
                    val rc  = obj.rarity.color

                    drawCircle(rc.copy(alpha = if (isSelected) 0.25f else 0.12f), r + 8f, scr)
                    drawCircle(Color(0xF0080D08), r - 2f, scr)
                    drawCircle(rc.copy(alpha = if (isSelected) 1f else 0.7f), r - 2f, scr,
                        style = Stroke(width = if (isSelected) 3.5f else 2f))
                }
            }
        }

        // ── Emoji markers layer ───────────────────────────────────────────
        if (mv != null) {
            val t = tick
            val density = androidx.compose.ui.platform.LocalDensity.current
            ecoObjects.forEach { obj ->
                val scr = mv.geoToScreen(obj.lat, obj.lon)
                if (scr != null) {
                    val isSelected = selectedObject?.id == obj.id
                    val boxSizeDp  = if (isSelected) 56.dp else 44.dp
                    val boxSizePx  = with(density) { boxSizeDp.toPx() }
                    Box(
                        Modifier
                            .offset {
                                IntOffset(
                                    (scr.x - boxSizePx / 2).toInt(),
                                    (scr.y - boxSizePx / 2).toInt()
                                )
                            }
                            .size(boxSizeDp)
                            .pointerInput(obj.id) {
                                detectTapGestures {
                                    selectedObject = if (selectedObject?.id == obj.id) null else obj
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(obj.emoji, fontSize = if (isSelected) 24.sp else 20.sp)
                    }
                }
            }
        }

        // ── Top Bar ───────────────────────────────────────────────────────
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.weight(1f)
                        .background(Color(0xCC0D1A0D), RoundedCornerShape(14.dp))
                        .border(1.dp, EcoBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔍", fontSize = 13.sp)
                    Text("Search objects...", color = EcoTextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                MapTopBtn("⚙️") {}
                MapTopBtn("📍") {
                    mapViewRef?.controller?.animateTo(GeoPoint(userLat, userLng))
                }
            }
        }

        // ── Zoom buttons ──────────────────────────────────────────────────
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapTopBtn("+") { mapViewRef?.controller?.zoomIn() }
            MapTopBtn("−") { mapViewRef?.controller?.zoomOut() }
        }

        // ── Scan choice dialog ────────────────────────────────────────────
        AnimatedVisibility(
            visible = showScanChoice && selectedObject != null,
            enter   = fadeIn() + scaleIn(initialScale = 0.92f),
            exit    = fadeOut() + scaleOut(targetScale = 0.92f)
        ) {
            selectedObject?.let { obj ->
                ScanChoiceDialog(
                    obj          = obj,
                    onAutoScan   = { showScanChoice = false; onNavigateToScanner(obj, false) },
                    onCameraScan = { showScanChoice = false; onNavigateToScanner(obj, true) },
                    onDismiss    = { showScanChoice = false }
                )
            }
        }

        // ── Bottom ────────────────────────────────────────────────────────
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            AnimatedVisibility(
                visible = selectedObject != null && !showScanChoice,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut()
            ) {
                selectedObject?.let { obj ->
                    val scannerCd = GameState.scannerCdRemaining()
                    val plantCd   = GameState.plantCdRemaining(obj.id)
                    val canScan   = scannerCd == 0L && plantCd == 0L
                    val distM     = distanceM(userLat, userLng, obj.lat, obj.lon).toInt()

                    SelectedObjectCard(
                        obj         = obj,
                        canScan     = canScan,
                        scannerCdMs = scannerCd,
                        plantCdMs   = plantCd,
                        distM       = distM,
                        onDismiss   = { selectedObject = null },
                        onScan      = { showScanChoice = true }
                    )
                }
            }

            NearbyStrip(
                objects    = ecoObjects.sortedBy { distanceM(userLat, userLng, it.lat, it.lon) }.take(8),
                selectedId = selectedObject?.id,
                userLat    = userLat,
                userLng    = userLng,
                onSelect   = { selectedObject = if (selectedObject?.id == it.id) null else it }
            )
        }
    }
}

// ─── Utility ─────────────────────────────────────────────────────────────────

private fun Offset.getDistance(): Float = kotlin.math.sqrt(x * x + y * y)

// ─── Top Bar button ───────────────────────────────────────────────────────────

@Composable
fun MapTopBtn(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp)
            .background(Color(0xCC0D1A0D), RoundedCornerShape(12.dp))
            .border(1.dp, EcoBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 18.sp, color = EcoTextPrimary) }
}

// ─── Scan choice dialog ───────────────────────────────────────────────────────

@Composable
fun ScanChoiceDialog(obj: MapObject, onAutoScan: () -> Unit, onCameraScan: () -> Unit, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            Modifier.fillMaxWidth().padding(28.dp).clickable(enabled = false) {},
            shape  = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = EcoSurface),
            border = BorderStroke(1.dp, obj.rarity.color.copy(alpha = 0.5f))
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(obj.emoji, fontSize = 52.sp)
                Text(obj.name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = EcoTextPrimary, textAlign = TextAlign.Center)
                Box(
                    Modifier.background(obj.rarity.color.copy(.12f), RoundedCornerShape(8.dp))
                        .border(1.dp, obj.rarity.color.copy(.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("★", fontSize = 9.sp, color = obj.rarity.color)
                        Text(obj.rarity.label.uppercase(), fontSize = 9.sp, color = obj.rarity.color, fontFamily = FontFamily.Monospace)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(EcoBorder))
                Text("How do you want to scan?", fontSize = 11.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)

                Button(
                    onClick = onCameraScan,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoGreenDark, contentColor = EcoBackground)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷  Camera + Plant.id", fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text("Photograph the plant → AI will identify it", fontSize = 9.sp, color = EcoBackground.copy(0.7f))
                    }
                }

                OutlinedButton(
                    onClick = onAutoScan,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EcoBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EcoTextPrimary)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚡  Auto-Scan", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Get the card without the camera", fontSize = 9.sp, color = EcoTextMuted)
                    }
                }

                Text("+${obj.rarity.multiplier} ECO per scan", fontSize = 10.sp, color = EcoGold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ─── Selected Object Card ─────────────────────────────────────────────────────

@Composable
fun SelectedObjectCard(obj: MapObject, canScan: Boolean, scannerCdMs: Long, plantCdMs: Long,
                       distM: Int, userLat: Double = 0.0, userLng: Double = 0.0,
                       onDismiss: () -> Unit, onScan: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EcoSurface),
        border = BorderStroke(1.dp, obj.rarity.color.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp)
                        .background(androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(obj.rarity.color.copy(0.2f), Color.Transparent)), CircleShape)
                        .border(1.dp, obj.rarity.color.copy(0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(obj.emoji, fontSize = 28.sp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(obj.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = EcoTextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RarityChip(obj.rarity)
                        Text("📍 ${distM} m", fontSize = 10.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                        Text("🪙 +${obj.rarity.multiplier}", fontSize = 10.sp, color = EcoGold, fontFamily = FontFamily.Monospace)
                        Text(obj.biome.emoji, fontSize = 11.sp)
                    }
                }
                Box(
                    Modifier.size(28.dp).background(EcoSurface2, CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) { Text("✕", fontSize = 11.sp, color = EcoTextMuted) }
            }

            if (!canScan) {
                Spacer(Modifier.height(10.dp))
                val (icon, text, clr) = when {
                    plantCdMs > 0 -> Triple("🔒", "Already scanned · CD: ${GameState.formatCd(plantCdMs)}", EcoRed)
                    else          -> Triple("⏳", "Scanner on cooldown · ${GameState.formatCd(scannerCdMs)}", EcoGold)
                }
                Row(
                    Modifier.fillMaxWidth()
                        .background(clr.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .border(1.dp, clr.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(icon, fontSize = 14.sp)
                    Text(text, fontSize = 10.sp, color = clr, fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onScan, enabled = canScan,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = EcoGreenDark, contentColor = EcoBackground,
                    disabledContainerColor = Color(0xFF1A2E1A), disabledContentColor = EcoTextMuted
                )
            ) {
                Text(if (canScan) "📷  Scan" else "🔒  Unavailable", fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun RarityChip(rarity: Rarity) {
    Box(
        Modifier.background(rarity.color.copy(0.12f), RoundedCornerShape(6.dp))
            .border(1.dp, rarity.color.copy(0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) { Text(rarity.label, fontSize = 9.sp, color = rarity.color, fontFamily = FontFamily.Monospace) }
}

// ─── Nearby Strip ─────────────────────────────────────────────────────────────

@Composable
fun NearbyStrip(objects: List<MapObject>, selectedId: Int?, userLat: Double, userLng: Double, onSelect: (MapObject) -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(Color.Transparent, EcoBackground.copy(alpha = 0.97f))))
            .padding(top = 10.dp, bottom = 4.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("⚡", fontSize = 12.sp)
                Text("Nearby objects", fontSize = 11.sp, color = EcoGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Text("${objects.size} found", fontSize = 10.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            objects.forEach { obj ->
                val distM      = distanceM(userLat, userLng, obj.lat, obj.lon).toInt()
                val onCd       = GameState.plantCdRemaining(obj.id) > 0
                val isSelected = selectedId == obj.id
                NearbyChip(obj, isSelected, distM, onCd) { onSelect(obj) }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
fun NearbyChip(obj: MapObject, isSelected: Boolean, distM: Int, onCooldown: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .background(if (isSelected) obj.rarity.color.copy(0.14f) else Color(0xCC0D1A0D), RoundedCornerShape(14.dp))
            .border(1.dp, if (isSelected) obj.rarity.color else EcoBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            Text(obj.emoji, fontSize = 20.sp)
            if (onCooldown) Box(
                Modifier.size(12.dp).align(Alignment.BottomEnd).background(EcoSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("🔒", fontSize = 7.sp) }
        }
        Column {
            Text(obj.name.split(" ").first(), fontSize = 11.sp,
                color = if (onCooldown) EcoTextMuted else EcoTextPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                if (onCooldown) "CD ${GameState.formatCd(GameState.plantCdRemaining(obj.id))}"
                else "${distM} m · ${obj.biome.emoji}",
                fontSize = 9.sp,
                color = if (onCooldown) EcoRed.copy(alpha = 0.7f) else EcoTextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}