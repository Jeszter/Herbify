package com.example.ecoscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.ecoscanner.ui.theme.MyApplicationTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView

class MainActivity : ComponentActivity() {
    private var cameraExecutor: ExecutorService? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Разрешение получено
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Проверяем разрешение камеры
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Разрешение уже есть
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            MyApplicationTheme {
                EcoScannerApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
    }
}

// Цветовая палитра
val GreenPrimary = Color(0xFF2E7D32)
val GreenLight = Color(0xFF4CAF50)
val BackgroundBeige = Color(0xFFF5F5DC)
val CardGreen = Color(0xFFE8F5E8)
val Gold = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoScannerApp() {
    var coins by remember { mutableStateOf(156) }
    var collectedItems by remember { mutableStateOf(listOf(
        "🌳 Дуб великий", "🌼 Ромашка полевая", "🌲 Сосна лесная",
        "🍃 Папоротник", "🌿 Мята перечная"
    )) }
    var currentScreen by remember { mutableStateOf("camera") }

    Scaffold(
        modifier = Modifier.background(BackgroundBeige),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "EcoScanner",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Магазин") },
                    label = { Text("Магазин") },
                    selected = currentScreen == "shop",
                    onClick = { currentScreen = "shop" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Сканер") },
                    label = { Text("Сканер") },
                    selected = currentScreen == "camera",
                    onClick = { currentScreen = "camera" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Collections, contentDescription = "Коллекция") },
                    label = { Text("Коллекция") },
                    selected = currentScreen == "collection",
                    onClick = { currentScreen = "collection" }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .background(BackgroundBeige)
        ) {
            when (currentScreen) {
                "camera" -> CameraScreen(
                    coins = coins,
                    onCoinsUpdate = { newCoins -> coins = newCoins },
                    onItemCollected = { newItem ->
                        collectedItems = collectedItems + newItem
                    }
                )
                "shop" -> ShopScreen(coins = coins)
                "collection" -> CollectionScreen(collectedItems = collectedItems)
            }
        }
    }
}

@Composable
fun CameraScreen(
    coins: Int,
    onCoinsUpdate: (Int) -> Unit,
    onItemCollected: (String) -> Unit
) {
    val context = LocalContext.current
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBeige)
    ) {
        // Верхняя панель приложения
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Сканер растений",
                        color = GreenPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Баланс: $coins ECO",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Gold, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌱", fontSize = 20.sp)
                }
            }
        }

        // Область камеры с рамкой
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (hasCameraPermission) {
                    CameraPreview()
                } else {
                    // Заглушка если нет разрешения
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📷",
                                fontSize = 48.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = "Разрешите доступ к камере",
                                color = Color.White,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "в настройках устройства",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Перекрестие для наведения
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Текст подсказки
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Наведите на растение",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(100.dp))

                        // Простое перекрестие
                        Box(
                            modifier = Modifier.size(200.dp)
                        ) {
                            // Горизонтальная линия
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(Color.White.copy(alpha = 0.7f))
                                    .align(Alignment.Center)
                            )
                            // Вертикальная линия
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(2.dp)
                                    .background(Color.White.copy(alpha = 0.7f))
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }

        // Панель управления
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Кнопка сканирования
            Button(
                onClick = {
                    val plants = listOf("🌺 Орхидея", "🌻 Подсолнух", "🍀 Клевер", "🎋 Бамбук", "🌹 Роза")
                    val newItem = plants.random()
                    onItemCollected(newItem)
                    onCoinsUpdate(coins + 10)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "📸 Сканировать растение",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Подсказка
            Text(
                text = "Убедитесь, что растение в фокусе и хорошо освещено",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun CameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    // Игнорируем ошибки в эмуляторе
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    )
}

@Composable
fun ShopScreen(coins: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBeige)
            .padding(16.dp)
    ) {
        Text(
            text = "Магазин улучшений",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = GreenPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Доступно: $coins ECO",
            fontSize = 18.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn {
            item {
                ShopItemCard(
                    icon = "🔍",
                    title = "Улучшенный сканер",
                    price = "50 ECO",
                    description = "Точность +20%"
                )
            }
            item {
                ShopItemCard(
                    icon = "🎒",
                    title = "Большая коллекция",
                    price = "80 ECO",
                    description = "+10 слотов для растений"
                )
            }
            item {
                ShopItemCard(
                    icon = "⚡",
                    title = "Быстрое сканирование",
                    price = "120 ECO",
                    description = "Сканирование в 2 раза быстрее"
                )
            }
            item {
                ShopItemCard(
                    icon = "🌟",
                    title = "Редкие растения",
                    price = "200 ECO",
                    description = "Доступ к эксклюзивным видам"
                )
            }
        }
    }
}

@Composable
fun ShopItemCard(icon: String, title: String, price: String, description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(price, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun CollectionScreen(collectedItems: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBeige)
            .padding(16.dp)
    ) {
        Text(
            text = "Моя коллекция",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = GreenPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Найдено: ${collectedItems.size} растений",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn {
            items(collectedItems) { item ->
                CollectionItemCard(item)
            }
        }
    }
}

@Composable
fun CollectionItemCard(plant: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = plant.substringBefore(" "),
                fontSize = 32.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = plant.substringAfter(" "),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = GreenPrimary
            )
        }
    }
}