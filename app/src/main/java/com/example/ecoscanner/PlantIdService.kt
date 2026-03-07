package com.example.ecoscanner

// ─── Add to app/build.gradle (dependencies section) ──────────────────────────
//
//   implementation("com.squareup.okhttp3:okhttp:4.12.0")
//   implementation("com.google.code.gson:gson:2.10.1")
//   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
//
// Add to AndroidManifest.xml (before the <application> tag):
//   <uses-permission android:name="android.permission.INTERNET"/>
//
// ─────────────────────────────────────────────────────────────────────────────

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// ─── Data classes ─────────────────────────────────────────────────────────────

data class PlantMatch(
    val name: String,
    val latinName: String,
    val probability: Float,
    val description: String,
    val family: String = "",
    val commonNames: List<String> = emptyList()
)

data class PlantIdResult(
    val topMatch: PlantMatch,
    val alternatives: List<PlantMatch>,
    val isPlant: Boolean,
    val confidence: Float,
    val matchedCard: EcoCard?
)

sealed class PlantIdError {
    object NoInternet     : PlantIdError()
    object NotAPlant      : PlantIdError()
    object LowConfidence  : PlantIdError()
    data class ApiError(val message: String) : PlantIdError()
}

sealed class PlantIdResponse {
    data class Success(val result: PlantIdResult) : PlantIdResponse()
    data class Error(val error: PlantIdError)     : PlantIdResponse()
}

// ─── Service ──────────────────────────────────────────────────────────────────

object PlantIdService {

    // Key: https://plant.id/ — free tier: 100 requests/day
    private const val API_KEY         = "YOUR_PLANT_ID_API_KEY"
    private const val API_URL         = "https://api.plant.id/v3/identification"
    private const val MIN_CONFIDENCE  = 0.20f

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // ── Main call (suspend — call from a coroutine) ───────────────────────

    suspend fun identify(bitmap: Bitmap): PlantIdResponse = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap)

            val requestBody = mapOf(
                "images"               to listOf(base64Image),
                "similar_images"       to true,
                "classification_raw"   to false,
                "classification_level" to "species"
            )

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Api-Key", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext PlantIdResponse.Error(
                    PlantIdError.ApiError("HTTP ${response.code}: ${response.message}")
                )
            }

            val json = response.body?.string()
                ?: return@withContext PlantIdResponse.Error(
                    PlantIdError.ApiError("Empty server response")
                )

            parseResponse(json)

        } catch (e: java.net.UnknownHostException) {
            PlantIdResponse.Error(PlantIdError.NoInternet)
        } catch (e: Exception) {
            PlantIdResponse.Error(PlantIdError.ApiError(e.message ?: "Unknown error"))
        }
    }

    // ── Parsing ───────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun parseResponse(json: String): PlantIdResponse {
        return try {
            val root        = gson.fromJson(json, Map::class.java) as Map<String, Any>
            val resultMap   = root["result"]         as? Map<String, Any>
            val isPlantMap  = resultMap?.get("is_plant") as? Map<String, Any>
            val isPlant     = (isPlantMap?.get("binary") as? Boolean) ?: false

            if (!isPlant) return PlantIdResponse.Error(PlantIdError.NotAPlant)

            val classiMap   = resultMap?.get("classification") as? Map<String, Any>
            val suggestions = (classiMap?.get("suggestions") as? List<*>)
                ?.filterIsInstance<Map<String, Any>>()
                ?: return PlantIdResponse.Error(PlantIdError.ApiError("No results"))

            val matches = suggestions.mapNotNull { s ->
                try {
                    val details     = s["details"]   as? Map<String, Any>
                    val descMap     = details?.get("description") as? Map<String, Any>
                    val probability = (s["probability"] as? Double)?.toFloat() ?: 0f
                    val commonRaw   = details?.get("common_names") as? List<*>
                    val taxonomy    = details?.get("taxonomy") as? Map<String, Any>

                    PlantMatch(
                        name        = (s["name"] as? String) ?: "Unknown",
                        latinName   = (details?.get("name_authority") as? String) ?: "",
                        probability = probability,
                        description = (descMap?.get("value") as? String) ?: "",
                        family      = (taxonomy?.get("family") as? String) ?: "",
                        commonNames = commonRaw?.filterIsInstance<String>() ?: emptyList()
                    )
                } catch (e: Exception) { null }
            }

            if (matches.isEmpty()) {
                return PlantIdResponse.Error(PlantIdError.ApiError("Could not identify"))
            }

            val top = matches.first()
            if (top.probability < MIN_CONFIDENCE) {
                return PlantIdResponse.Error(PlantIdError.LowConfidence)
            }

            PlantIdResponse.Success(
                PlantIdResult(
                    topMatch     = top,
                    alternatives = matches.drop(1).take(3),
                    isPlant      = true,
                    confidence   = top.probability,
                    matchedCard  = matchToCard(top)
                )
            )

        } catch (e: Exception) {
            PlantIdResponse.Error(PlantIdError.ApiError("Parse error: ${e.message}"))
        }
    }

    // ── Match to card database ────────────────────────────────────────────

    fun matchToCard(match: PlantMatch): EcoCard? {
        val latinLower = match.latinName.lowercase()
        val nameLower  = match.name.lowercase()

        // 1. By Latin name
        PLANT_DATABASE.firstOrNull { card ->
            val cl = card.latin.lowercase()
            latinLower.contains(cl) || cl.contains(latinLower)
        }?.let { return it }

        // 2. By common name (words longer than 4 characters)
        PLANT_DATABASE.firstOrNull { card ->
            card.name.split(" ").any { word ->
                word.length > 4 && nameLower.contains(word.lowercase())
            }
        }?.let { return it }

        // 3. Create a card on the fly
        return EcoCard(
            id          = match.name.hashCode().and(0x7FFFFFFF),
            emoji       = "🌿",
            name        = match.name,
            latin       = match.latinName,
            rarity      = rarityFromConfidence(match.probability),
            description = match.description.take(200).ifBlank { "New species in your collection!" }
        )
    }

    private fun rarityFromConfidence(confidence: Float): Rarity = when {
        confidence >= 0.90f -> Rarity.LEGENDARY
        confidence >= 0.75f -> Rarity.EPIC
        confidence >= 0.50f -> Rarity.RARE
        else                -> Rarity.COMMON
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out    = ByteArrayOutputStream()
        val scaled = scaleBitmap(bitmap, 800)
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return "data:image/jpeg;base64," +
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val scale = maxDim.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }
}