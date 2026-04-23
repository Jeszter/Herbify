package com.herbify.app.game

import android.content.Context
import android.net.Uri
import android.util.Log
import com.herbify.app.BuildConfig
import com.herbify.app.model.Rarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object PlantNetService {

    private val apiKey: String
        get() = BuildConfig.PLANTNET_API_KEY

    private const val baseUrl = "https://my-api.plantnet.org/v2/identify/all"
    private const val minConfidence = 0.15f
    private const val tag = "PlantNetService"

    data class PlantCandidate(
        val scientificName: String,
        val commonName: String,
        val confidence: Float,
        val family: String,
        val genus: String,
        val wikiUrl: String?
    )

    data class PlantNetResponse(
        val topCandidate: PlantCandidate?,
        val candidates: List<PlantCandidate>,
        val bestMatch: String?,
        val remainingIdentificationRequests: Int?,
        val success: Boolean,
        val errorMessage: String? = null
    )

    suspend fun identify(imageUri: Uri, context: Context): PlantNetResponse {
        return withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext PlantNetResponse(
                    topCandidate = null,
                    candidates = emptyList(),
                    bestMatch = null,
                    remainingIdentificationRequests = null,
                    success = false,
                    errorMessage = "Missing PlantNet API key"
                )
            }

            try {
                val imageFile = uriToFile(imageUri, context)
                    ?: return@withContext PlantNetResponse(
                        topCandidate = null,
                        candidates = emptyList(),
                        bestMatch = null,
                        remainingIdentificationRequests = null,
                        success = false,
                        errorMessage = "Could not read image"
                    )

                val result = callPlantNetApi(imageFile)
                imageFile.delete()
                result
            } catch (e: Exception) {
                Log.e(tag, "PlantNet error: ${e.message}", e)
                PlantNetResponse(
                    topCandidate = null,
                    candidates = emptyList(),
                    bestMatch = null,
                    remainingIdentificationRequests = null,
                    success = false,
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }

    private fun callPlantNetApi(imageFile: File): PlantNetResponse {
        val boundary = "----HerbifyBoundary${System.currentTimeMillis()}"
        val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
        val url = URL("$baseUrl?api-key=$encodedKey&lang=en&no-reject=true&nb-results=3")

        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connectTimeout = 15000
            readTimeout = 30000
        }

        DataOutputStream(connection.outputStream).use { out ->
            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"images\"; filename=\"plant.jpg\"\r\n")
            out.writeBytes("Content-Type: image/jpeg\r\n\r\n")
            out.write(imageFile.readBytes())
            out.writeBytes("\r\n")
            out.writeBytes("--$boundary--\r\n")
            out.flush()
        }

        val responseCode = connection.responseCode

        if (responseCode != 200) {
            val error = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            Log.e(tag, "PlantNet HTTP error: $error")
            return PlantNetResponse(
                topCandidate = null,
                candidates = emptyList(),
                bestMatch = null,
                remainingIdentificationRequests = null,
                success = false,
                errorMessage = "API error: HTTP $responseCode"
            )
        }

        val responseText = connection.inputStream.bufferedReader().readText()
        return parseResponse(responseText)
    }

    private fun parseResponse(json: String): PlantNetResponse {
        return try {
            val root = JSONObject(json)
            val results = root.optJSONArray("results")
            val bestMatch = root.optString("bestMatch", null)
            val remainingRequests =
                if (root.has("remainingIdentificationRequests")) {
                    root.optInt("remainingIdentificationRequests")
                } else {
                    null
                }

            if (results == null || results.length() == 0) {
                return PlantNetResponse(
                    topCandidate = null,
                    candidates = emptyList(),
                    bestMatch = bestMatch,
                    remainingIdentificationRequests = remainingRequests,
                    success = false,
                    errorMessage = "Plant not recognized"
                )
            }

            val candidates = mutableListOf<PlantCandidate>()

            for (i in 0 until minOf(results.length(), 3)) {
                val item = results.getJSONObject(i)
                val score = item.optDouble("score", 0.0).toFloat()
                val species = item.getJSONObject("species")

                val scientificName = species.optString("scientificNameWithoutAuthor", "")
                    .ifBlank { species.optString("scientificName", "") }

                val commonNames = species.optJSONArray("commonNames")
                val commonName =
                    if (commonNames != null && commonNames.length() > 0) {
                        commonNames.getString(0)
                    } else {
                        scientificName
                    }

                val familyObj = species.optJSONObject("family")
                val family = familyObj?.optString("scientificNameWithoutAuthor", "") ?: ""

                val genusObj = species.optJSONObject("genus")
                val genus = genusObj?.optString("scientificNameWithoutAuthor", "") ?: ""

                val gbif = item.optJSONObject("gbif")
                val wikiUrl = gbif?.optString("id")?.takeIf { it.isNotBlank() }?.let {
                    "https://www.gbif.org/species/$it"
                }

                candidates.add(
                    PlantCandidate(
                        scientificName = scientificName,
                        commonName = commonName,
                        confidence = score,
                        family = family,
                        genus = genus,
                        wikiUrl = wikiUrl
                    )
                )
            }

            val top = candidates.firstOrNull()

            if (top == null || top.confidence < minConfidence) {
                return PlantNetResponse(
                    topCandidate = top,
                    candidates = candidates,
                    bestMatch = bestMatch,
                    remainingIdentificationRequests = remainingRequests,
                    success = false,
                    errorMessage = if (top == null) {
                        "Plant not recognized"
                    } else {
                        "Confidence too low (${(top.confidence * 100).toInt()}%). Try a clearer photo."
                    }
                )
            }

            PlantNetResponse(
                topCandidate = top,
                candidates = candidates,
                bestMatch = bestMatch,
                remainingIdentificationRequests = remainingRequests,
                success = true,
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e(tag, "Parse error: ${e.message}", e)
            PlantNetResponse(
                topCandidate = null,
                candidates = emptyList(),
                bestMatch = null,
                remainingIdentificationRequests = null,
                success = false,
                errorMessage = "Response parsing failed"
            )
        }
    }

    private fun uriToFile(uri: Uri, context: Context): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("herbify_scan_", ".jpg", context.cacheDir)
            tempFile.outputStream().use { out ->
                inputStream.copyTo(out)
            }
            inputStream.close()

            if (tempFile.length() > 1_000_000) {
                compressImage(tempFile, context)
            } else {
                tempFile
            }
        } catch (e: Exception) {
            Log.e(tag, "File conversion error: ${e.message}", e)
            null
        }
    }

    private fun compressImage(file: File, context: Context): File {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)

            val maxSize = 800
            val ratio = maxOf(bitmap.width, bitmap.height).toFloat() / maxSize
            val newWidth = (bitmap.width / ratio).toInt()
            val newHeight = (bitmap.height / ratio).toInt()

            val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            bitmap.recycle()

            val compressed = File.createTempFile("herbify_compressed_", ".jpg", context.cacheDir)
            compressed.outputStream().use { out ->
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
            }
            scaled.recycle()
            file.delete()
            compressed
        } catch (_: Exception) {
            file
        }
    }

    fun mapConfidenceToRarity(confidence: Float): Rarity {
        return when {
            confidence >= 0.90f -> Rarity.COMMON
            confidence >= 0.75f -> Rarity.UNCOMMON
            confidence >= 0.55f -> Rarity.RARE
            confidence >= 0.40f -> Rarity.EPIC
            else -> Rarity.LEGENDARY
        }
    }
}