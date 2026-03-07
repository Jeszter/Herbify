package com.example.ecoscanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ecoscanner.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Scanner states ───────────────────────────────────────────────────────────

sealed class ScanState {
    object Idle       : ScanState()
    object Analyzing  : ScanState()
    data class Result(val card: EcoCard, val confidence: Float, val alternatives: List<Pair<String, Float>>) : ScanState()
    data class Error(val message: String) : ScanState()
}

// ─── ScannerScreen ────────────────────────────────────────────────────────────

@Composable
fun ScannerScreen(
    pendingObject: MapObject?    = null,
    useCamera: Boolean           = true,
    onScan: (EcoCard) -> Unit,
    onClearPending: () -> Unit   = {}
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope          = rememberCoroutineScope()

    val hasCamera = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    var scanState by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    // CD ticker
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(1000); tick = System.currentTimeMillis() } }

    val scannerCd = GameState.scannerCdRemaining()
    val plantCd   = pendingObject?.let { GameState.plantCdRemaining(it.id) } ?: 0L
    val canScan   = scannerCd == 0L && plantCd == 0L

    // Auto-scan (no camera)
    fun doAutoScan() {
        if (!canScan) return
        scope.launch {
            scanState = ScanState.Analyzing
            delay(1800)
            val card = pendingObject?.toEcoCard() ?: PLANT_DATABASE.random()
            scanState = ScanState.Result(
                card         = card,
                confidence   = (75..99).random() / 100f,
                alternatives = listOf(
                    (PLANT_DATABASE.random().name to 0.04f),
                    (PLANT_DATABASE.random().name to 0.02f)
                )
            )
        }
    }

    // Camera scan
    fun doCameraScan(bitmap: Bitmap) {
        scope.launch {
            scanState = ScanState.Analyzing
            val response = PlantIdService.identify(bitmap)
            scanState = when (response) {
                is PlantIdResponse.Success -> {
                    val r = response.result
                    ScanState.Result(
                        card         = r.matchedCard ?: PLANT_DATABASE.random(),
                        confidence   = r.confidence,
                        alternatives = r.alternatives.map { it.name to it.probability }
                    )
                }
                is PlantIdResponse.Error -> {
                    val card = pendingObject?.toEcoCard() ?: PLANT_DATABASE.random()
                    ScanState.Result(card, 0.87f, emptyList())
                }
            }
        }
    }

    // Auto-mode — start immediately
    LaunchedEffect(pendingObject) {
        if (pendingObject != null && !useCamera) {
            delay(300)
            doAutoScan()
        }
    }

    Column(
        Modifier.fillMaxSize().background(EcoBackground)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    if (pendingObject != null) "${pendingObject.emoji} ${pendingObject.name}" else "EcoScanner",
                    fontSize = 20.sp, fontWeight = FontWeight.Black, color = EcoGreen
                )
                Text(
                    if (pendingObject != null) pendingObject.rarity.label else "AR · Scanner",
                    fontSize = 10.sp,
                    color = pendingObject?.rarity?.color ?: EcoTextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
            // AI badge
            Row(
                Modifier
                    .background(Color(0xFF0A1A0A), RoundedCornerShape(20.dp))
                    .border(1.dp, EcoGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(Modifier.size(6.dp).background(EcoGreen, CircleShape))
                Text("AI", fontSize = 9.sp, color = EcoGreen, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            }
        }

        // ── Camera ─────────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF061406))
                .border(1.dp, pendingObject?.rarity?.color?.copy(alpha = 0.3f) ?: EcoGreenDim, RoundedCornerShape(20.dp))
        ) {
            when {
                hasCamera && useCamera -> {
                    AndroidView(
                        factory = { ctx ->
                            val pv = PreviewView(ctx)
                            val imgCapture = ImageCapture.Builder().build()
                            imageCaptureRef = imgCapture
                            ProcessCameraProvider.getInstance(ctx).addListener({
                                try {
                                    val cp = ProcessCameraProvider.getInstance(ctx).get()
                                    val preview = Preview.Builder().build()
                                    preview.setSurfaceProvider(pv.surfaceProvider)
                                    cp.unbindAll()
                                    cp.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imgCapture
                                    )
                                } catch (_: Exception) {}
                            }, ContextCompat.getMainExecutor(ctx))
                            pv
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Box(Modifier.fillMaxSize().background(Color(0xFF061406)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(pendingObject?.emoji ?: "🌿", fontSize = 72.sp)
                            if (scanState is ScanState.Analyzing) {
                                Text("Analyzing...", color = EcoGreen, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // Scan corners
            ScanCorners(
                color    = pendingObject?.rarity?.color ?: EcoGreen,
                scanning = scanState is ScanState.Analyzing
            )

            // Hint
            if (scanState is ScanState.Idle) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(Color.Black.copy(0.65f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        if (useCamera) "Point at the plant"
                        else "Press Auto-Scan",
                        color = EcoTextPrimary, fontSize = 12.sp
                    )
                }
            }

            // Analysis progress bar
            if (scanState is ScanState.Analyzing) {
                AnalyzingBar(Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth())
            }
        }

        // ── Plant.id result row ────────────────────────────────────────────
        AnimatedVisibility(
            visible = scanState is ScanState.Result,
            enter   = slideInVertically { it } + fadeIn()
        ) {
            val result = scanState as? ScanState.Result
            if (result != null) {
                PlantIdResultPanel(
                    card         = result.card,
                    confidence   = result.confidence,
                    alternatives = result.alternatives,
                    onCollect    = {
                        GameState.recordScan(result.card)
                        onScan(result.card)
                        scanState = ScanState.Idle
                    }
                )
            }
        }

        // ── CD / Buttons ───────────────────────────────────────────────────
        if (scanState !is ScanState.Result) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Cooldown
                if (!canScan && scanState is ScanState.Idle) {
                    val (icon, text, clr) = when {
                        plantCd > 0 -> Triple("🔒", "Already scanned · CD: ${GameState.formatCd(plantCd)}", EcoRed)
                        else        -> Triple("⏳", "Scanner on cooldown · ${GameState.formatCd(scannerCd)}", EcoGold)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(clr.copy(0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, clr.copy(0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(icon, fontSize = 16.sp)
                        Text(text, fontSize = 10.sp, color = clr, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (useCamera) {
                        Button(
                            onClick = {
                                if (!canScan || scanState is ScanState.Analyzing) return@Button
                                imageCaptureRef?.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bmp = image.toBitmap()
                                            image.close()
                                            doCameraScan(bmp)
                                        }
                                        override fun onError(e: ImageCaptureException) { doAutoScan() }
                                    }
                                ) ?: doAutoScan()
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            enabled  = canScan && scanState !is ScanState.Analyzing,
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor        = EcoGreenDark,
                                contentColor          = EcoBackground,
                                disabledContainerColor= Color(0xFF1A2E1A),
                                disabledContentColor  = EcoTextMuted
                            )
                        ) {
                            Text(
                                when (scanState) {
                                    is ScanState.Analyzing -> "⏳ Analyzing..."
                                    else -> if (canScan) "📷 Take Photo" else "🔒 Unavailable"
                                },
                                fontSize = 14.sp, fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Auto-scan
                    OutlinedButton(
                        onClick  = { doAutoScan() },
                        modifier = Modifier.height(52.dp).let { if (useCamera) it else it.fillMaxWidth() },
                        enabled  = canScan && scanState !is ScanState.Analyzing,
                        shape    = RoundedCornerShape(14.dp),
                        border   = BorderStroke(1.dp, if (canScan) EcoBorder else Color(0xFF1A2A1A)),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = EcoTextPrimary)
                    ) {
                        Text("⚡ Auto", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Scanner corners ──────────────────────────────────────────────────────────

@Composable
fun ScanCorners(color: Color, scanning: Boolean) {
    val inf   = rememberInfiniteTransition(label = "c")
    val alpha by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    val scanY by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "sy")
    val c     = color.copy(alpha = if (scanning) 1f else alpha)

    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val cs  = 32.dp.toPx(); val sw = 3.dp.toPx()
        val pad = size.width * 0.12f
        val l = pad; val r = size.width - pad
        val t = size.height * 0.15f; val b = size.height * 0.85f

        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(c, Offset(x, y), Offset(x + dx * cs, y), strokeWidth = sw)
            drawLine(c, Offset(x, y), Offset(x, y + dy * cs), strokeWidth = sw)
        }
        corner(l, t, 1f, 1f); corner(r, t, -1f, 1f)
        corner(l, b, 1f, -1f); corner(r, b, -1f, -1f)

        // Scan line
        if (scanning) {
            val sy = t + (b - t) * scanY
            drawLine(c.copy(alpha = 0.7f), Offset(l, sy), Offset(r, sy), strokeWidth = 2.dp.toPx())
        }
    }
}

// ─── Analysis bar ─────────────────────────────────────────────────────────────

@Composable
fun AnalyzingBar(modifier: Modifier) {
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (progress < 1f) { delay(20); progress = (progress + 0.011f).coerceAtMost(1f) }
    }
    Column(
        modifier
            .background(EcoSurface.copy(alpha = 0.93f), RoundedCornerShape(14.dp))
            .border(1.dp, EcoBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).background(EcoGreen, CircleShape))
                Text("Plant.id · Analysis complete", color = EcoGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Text("${(progress * 100).toInt()}%", color = EcoGold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(
            progress  = progress,
            modifier  = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color     = EcoGreen, trackColor = EcoGreenDim.copy(0.3f)
        )
    }
}

// ─── Plant.id result panel ────────────────────────────────────────────────────

@Composable
fun PlantIdResultPanel(
    card: EcoCard,
    confidence: Float,
    alternatives: List<Pair<String, Float>>,
    onCollect: () -> Unit
) {
    val pct = (confidence * 100).toInt()
    val barColor = when {
        pct >= 80 -> EcoGreen
        pct >= 50 -> EcoGold
        else      -> EcoRed
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Plant.id status row
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(8.dp).background(EcoGreen, CircleShape))
            Text("Plant.id", color = EcoGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("·  Analysis complete  ·  ${pct}% confidence", color = EcoGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        // Confidence progress
        LinearProgressIndicator(
            progress  = confidence,
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
            color     = barColor, trackColor = EcoGreenDim.copy(0.2f)
        )
        Spacer(Modifier.height(8.dp))

        // Main result card
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape  = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = EcoSurface),
            border = BorderStroke(1.dp, card.rarity.color.copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(52.dp)
                            .background(Brush.radialGradient(listOf(card.rarity.color.copy(0.2f), Color.Transparent)), CircleShape)
                            .border(1.dp, card.rarity.color.copy(0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text(card.emoji, fontSize = 28.sp) }
                    Column(Modifier.weight(1f)) {
                        Text(card.name, fontSize = 16.sp, fontWeight = FontWeight.Black, color = EcoTextPrimary)
                        Text(card.latin, fontSize = 10.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                    }
                    // Rarity badge
                    Box(
                        Modifier
                            .background(card.rarity.color.copy(0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, card.rarity.color.copy(0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("★", fontSize = 9.sp, color = card.rarity.color)
                            Text(card.rarity.label.uppercase(), fontSize = 9.sp, color = card.rarity.color, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Tags
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    InfoTag("🌿 Angiosperms")
                    InfoTag("📏 10–30 cm")
                    if (card.rarity == Rarity.EPIC || card.rarity == Rarity.LEGENDARY) InfoTag("⚠️ Endangered")
                }

                Text(card.description, fontSize = 11.sp, color = EcoTextMuted, lineHeight = 16.sp)
            }
        }

        // Alternatives
        if (alternatives.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("OTHER SUGGESTIONS", fontSize = 9.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp), letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            alternatives.take(2).forEach { (name, prob) ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, fontSize = 12.sp, color = EcoTextPrimary, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .width(60.dp).height(4.dp)
                            .background(EcoGreenDim.copy(0.3f), RoundedCornerShape(2.dp))
                    ) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(prob * 5f).background(EcoGreenDim, RoundedCornerShape(2.dp)))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${(prob * 100).toInt()}%", fontSize = 9.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // "Add to Collection" button
        Button(
            onClick  = onCollect,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = EcoGreenDark, contentColor = EcoBackground)
        ) {
            Text("🃏  Add to Collection  +${card.rarity.multiplier} ECO", fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun InfoTag(text: String) {
    Box(
        Modifier
            .background(Color(0xFF0D1A0D), RoundedCornerShape(8.dp))
            .border(1.dp, EcoBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) { Text(text, fontSize = 9.sp, color = EcoTextMuted, fontFamily = FontFamily.Monospace) }
}