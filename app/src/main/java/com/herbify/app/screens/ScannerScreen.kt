package com.herbify.app.screens
import com.google.accompanist.permissions.PermissionStatus

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.herbify.app.BuildConfig
import com.herbify.app.game.GameState
import com.herbify.app.model.Zone
import com.herbify.app.ui.theme.DarkBg
import com.herbify.app.ui.theme.NeonGreen
import com.herbify.app.viewmodel.HerbariumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class RecognizedPlantUi(
    val plantName: String,
    val scientificName: String,
    val imageUrl: String?,
    val confidence: Double,
    val fact: String?
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    gameState: GameState,
    zone: Zone?,
    herbariumViewModel: HerbariumViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var recognizedPlant by remember { mutableStateOf<RecognizedPlantUi?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (permissionState.status !is PermissionStatus.Granted) {
            permissionState.launchPermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        if (permissionState.status is PermissionStatus.Granted) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Camera start failed"
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            DisposableEffect(lifecycleOwner) {
                onDispose {
                    try {
                        ProcessCameraProvider.getInstance(context).get().unbindAll()
                    } catch (_: Exception) {
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Scanner",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = "Take a clear photo of the plant",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        zone?.let {
                            Text(
                                text = "Zone: ${it.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonGreen
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Back",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    )
                }

                Surface(
                    onClick = {
                        val capture = imageCapture
                        if (capture != null && !isCapturing) {
                            isCapturing = true
                        }
                    },
                    shape = CircleShape,
                    color = NeonGreen,
                    modifier = Modifier.size(84.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(34.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.18f))
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent
                ) {
                    Box(modifier = Modifier.size(64.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera permission is required",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                TextButton(onClick = { permissionState.launchPermissionRequest() }) {
                    Text("Grant")
                }
                TextButton(onClick = onBack) {
                    Text("Back")
                }
            }
        }
    }

    if (isCapturing && recognizedPlant == null) {
        LaunchedEffect(isCapturing) {
            if (isCapturing) {
                val capture = imageCapture
                if (capture != null) {
                    takePhotoSuspend(context, capture).fold(
                        onSuccess = { file ->
                            val result = recognizePlant(file)
                            isCapturing = false
                            result.fold(
                                onSuccess = { recognizedPlant = it },
                                onFailure = { errorMessage = it.message ?: "Recognition failed" }
                            )
                            file.delete()
                        },
                        onFailure = {
                            isCapturing = false
                            errorMessage = it.message ?: "Capture failed"
                        }
                    )
                } else {
                    isCapturing = false
                }
            }
        }
    }

    recognizedPlant?.let { plant ->
        AlertDialog(
            onDismissRequest = { recognizedPlant = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        herbariumViewModel.saveRecognizedPlant(
                            plantName = plant.plantName,
                            scientificName = plant.scientificName,
                            imageUrl = plant.imageUrl,
                            zoneName = zone?.id,
                            fact = plant.fact,
                            confidence = plant.confidence
                        )
                        recognizedPlant = null
                    }
                ) {
                    Text("Save to Herbarium")
                }
            },
            dismissButton = {
                TextButton(onClick = { recognizedPlant = null }) {
                    Text("Close")
                }
            },
            title = {
                Text(plant.plantName)
            },
            text = {
                Column {
                    AsyncImage(
                        model = plant.imageUrl,
                        contentDescription = plant.plantName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )

                    if (plant.scientificName.isNotBlank()) {
                        Text(
                            text = "Scientific: ${plant.scientificName}",
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Text(
                        text = "Confidence: ${(plant.confidence * 100).toInt()}%",
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    if (!plant.fact.isNullOrBlank()) {
                        Text(
                            text = plant.fact,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            },
            title = {
                Text("Error")
            },
            text = {
                Text(message)
            }
        )
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSuccess: (File) -> Unit,
    onError: (String) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        "plant_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSuccess(photoFile)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "Capture error")
            }
        }
    )
}

private suspend fun takePhotoSuspend(
    context: Context,
    imageCapture: ImageCapture
): Result<File> = withContext(Dispatchers.Main) {
    kotlin.runCatching {
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val photoFile = File(
                context.cacheDir,
                "plant_${System.currentTimeMillis()}.jpg"
            )

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        if (continuation.isActive) continuation.resume(photoFile) {}
                    }

                    override fun onError(exception: ImageCaptureException) {
                        if (continuation.isActive) continuation.resumeWith(
                            Result.failure(exception)
                        )
                    }
                }
            )
        }
    }
}

private suspend fun recognizePlant(file: File): Result<RecognizedPlantUi> = withContext(Dispatchers.IO) {
    kotlin.runCatching {
        val plantNet = identifyWithPlantNet(file)
        val perenual = fetchPerenualData(plantNet.plantName, plantNet.scientificName)

        RecognizedPlantUi(
            plantName = perenual?.plantName ?: plantNet.plantName,
            scientificName = perenual?.scientificName ?: plantNet.scientificName,
            imageUrl = perenual?.imageUrl,
            confidence = plantNet.confidence,
            fact = perenual?.fact
        )
    }
}

private data class PlantNetResult(
    val plantName: String,
    val scientificName: String,
    val confidence: Double
)

private data class PerenualResult(
    val plantName: String,
    val scientificName: String,
    val imageUrl: String?,
    val fact: String?
)

private fun identifyWithPlantNet(file: File): PlantNetResult {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("organs", "leaf")
        .addFormDataPart(
            "images",
            file.name,
            file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        )
        .build()

    val request = Request.Builder()
        .url("https://my-api.plantnet.org/v2/identify/all?api-key=${BuildConfig.PLANTNET_API_KEY}")
        .post(requestBody)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IllegalStateException("PlantNet error ${response.code}")
        }

        val body = response.body?.string().orEmpty()
        val json = JSONObject(body)
        val results = json.optJSONArray("results")
        if (results == null || results.length() == 0) {
            throw IllegalStateException("Plant not recognized")
        }

        val first = results.getJSONObject(0)
        val score = first.optDouble("score", 0.0)
        val species = first.optJSONObject("species") ?: JSONObject()

        val scientificName = species.optString("scientificNameWithoutAuthor")
            .ifBlank { species.optString("scientificName") }

        val commonNames = species.optJSONArray("commonNames")
        val commonName = if (commonNames != null && commonNames.length() > 0) {
            commonNames.optString(0)
        } else {
            scientificName
        }

        return PlantNetResult(
            plantName = commonName.ifBlank { scientificName.ifBlank { "Unknown plant" } },
            scientificName = scientificName.ifBlank { commonName.ifBlank { "Unknown" } },
            confidence = score.coerceIn(0.0, 1.0)
        )
    }
}

private fun fetchPerenualData(plantName: String, scientificName: String): PerenualResult? {
    if (BuildConfig.PERENUAL_API_KEY.isBlank()) return null

    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val query = Uri.encode(scientificName.ifBlank { plantName })
    val request = Request.Builder()
        .url("https://perenual.com/api/v2/species-list?key=${BuildConfig.PERENUAL_API_KEY}&q=$query")
        .get()
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val body = response.body?.string().orEmpty()
        val json = JSONObject(body)
        val data = json.optJSONArray("data") ?: return null
        if (data.length() == 0) return null

        val first = data.getJSONObject(0)
        val commonName = first.optString("common_name").ifBlank { plantName }
        val scientificArray = first.optJSONArray("scientific_name")
        val scientific = if (scientificArray != null && scientificArray.length() > 0) {
            scientificArray.optString(0)
        } else {
            scientificName
        }

        val defaultImage = first.optJSONObject("default_image")
        val imageUrl =
            defaultImage?.optString("original_url").takeUnless { it.isNullOrBlank() }
                ?: defaultImage?.optString("regular_url").takeUnless { it.isNullOrBlank() }
                ?: defaultImage?.optString("medium_url").takeUnless { it.isNullOrBlank() }

        val family = first.optString("family")
        val genus = first.optString("genus")

        val fact = buildString {
            if (family.isNotBlank()) append("Family: $family. ")
            if (genus.isNotBlank()) append("Genus: $genus. ")
            if (scientific.isNotBlank()) append("Scientific name: $scientific.")
        }.ifBlank { null }

        return PerenualResult(
            plantName = commonName.ifBlank { scientific.ifBlank { plantName } },
            scientificName = scientific.ifBlank { scientificName.ifBlank { "Unknown" } },
            imageUrl = imageUrl,
            fact = fact
        )
    }
}