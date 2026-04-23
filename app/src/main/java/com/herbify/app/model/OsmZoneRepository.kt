package com.herbify.app.model

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.abs

object OsmZoneRepository {
    private const val TAG = "OsmZoneRepository"

    private val client = OkHttpClient()

    @Volatile
    private var cachedZones: List<Zone> = emptyList()

    fun getCachedZones(): List<Zone> = cachedZones

    suspend fun loadZonesAround(
        lat: Double,
        lng: Double,
        radiusMeters: Int = 850
    ): List<Zone> = withContext(Dispatchers.IO) {
        try {
            val query = buildOverpassQuery(lat, lng, radiusMeters)
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://overpass.kumi.systems/api/interpreter?data=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "Herbify/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                val body = response.body?.string().orEmpty()

                Log.d(TAG, "Overpass response code=$code")
                Log.d(TAG, "Overpass response body length=${body.length}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "Overpass failed: code=$code body=$body")
                    return@withContext cachedZones
                }

                if (body.isBlank()) {
                    Log.e(TAG, "Overpass returned blank body")
                    return@withContext cachedZones
                }

                val parsed = parseZones(body)
                val balanced = balanceZones(lat, lng, parsed)

                cachedZones = balanced

                Log.d(TAG, "Parsed zones count=${parsed.size}")
                Log.d(TAG, "Balanced zones count=${balanced.size}")

                return@withContext balanced
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadZonesAround failed", e)
            return@withContext cachedZones
        }
    }

    private fun buildOverpassQuery(lat: Double, lng: Double, radiusMeters: Int): String {
        return """
[out:json][timeout:25];
(
  way["natural"="wood"](around:$radiusMeters,$lat,$lng);
  way["landuse"="forest"](around:$radiusMeters,$lat,$lng);

  way["leisure"="park"](around:$radiusMeters,$lat,$lng);
  way["leisure"="garden"](around:$radiusMeters,$lat,$lng);
  way["leisure"="dog_park"](around:$radiusMeters,$lat,$lng);

  way["landuse"="meadow"](around:$radiusMeters,$lat,$lng);
  way["natural"="grassland"](around:$radiusMeters,$lat,$lng);
  way["natural"="heath"](around:$radiusMeters,$lat,$lng);

  way["natural"="water"](around:$radiusMeters,$lat,$lng);
  way["waterway"="riverbank"](around:$radiusMeters,$lat,$lng);
  way["leisure"="swimming_pool"](around:$radiusMeters,$lat,$lng);

  way["landuse"="grass"](around:$radiusMeters,$lat,$lng);
  way["natural"="scrub"](around:$radiusMeters,$lat,$lng);
  way["leisure"="common"](around:$radiusMeters,$lat,$lng);

  way["landuse"="industrial"](around:$radiusMeters,$lat,$lng);
  way["landuse"="brownfield"](around:$radiusMeters,$lat,$lng);
);
out geom;
        """.trimIndent()
    }

    private fun parseZones(json: String): List<Zone> {
        val root = JSONObject(json)
        val elements = root.optJSONArray("elements") ?: JSONArray()
        val zones = mutableListOf<Zone>()

        for (i in 0 until elements.length()) {
            val element = elements.optJSONObject(i) ?: continue
            if (element.optString("type") != "way") continue

            val zoneType = detectZoneType(element) ?: continue
            val geometry = element.optJSONArray("geometry") ?: continue

            val coordinates = geometryToCoordinates(geometry)
            if (coordinates.size < 4) continue

            val areaScore = approximateAreaScore(coordinates)

            val minArea = when (zoneType) {
                ZoneType.WATER -> 0.00000008
                ZoneType.PARK -> 0.00000010
                ZoneType.GARDEN -> 0.00000008
                ZoneType.MEADOW -> 0.00000010
                ZoneType.FOREST -> 0.00000012
                ZoneType.URBAN_GREEN -> 0.00000020
                ZoneType.RUINS -> 0.00000018
            }

            if (areaScore < minArea) continue

            val id = "way_${element.optLong("id")}"

            zones += Zone(
                id = id,
                type = zoneType,
                coordinates = coordinates
            )
        }

        return zones.distinctBy { it.id }
    }

    private fun detectZoneType(element: JSONObject): ZoneType? {
        val tags = element.optJSONObject("tags") ?: return null

        val natural = tags.optString("natural")
        val landuse = tags.optString("landuse")
        val leisure = tags.optString("leisure")
        val waterway = tags.optString("waterway")
        val railway = tags.optString("railway")

        return when {
            natural == "wood" || landuse == "forest" -> ZoneType.FOREST
            leisure == "park" -> ZoneType.PARK
            leisure == "garden" || leisure == "dog_park" -> ZoneType.GARDEN
            landuse == "meadow" || natural == "grassland" || natural == "heath" -> ZoneType.MEADOW
            natural == "water" || waterway == "riverbank" || leisure == "swimming_pool" -> ZoneType.WATER
            landuse == "grass" || natural == "scrub" || leisure == "common" -> ZoneType.URBAN_GREEN
            railway.isNotEmpty() || landuse == "industrial" || landuse == "brownfield" -> ZoneType.RUINS
            else -> null
        }
    }

    private fun geometryToCoordinates(geometry: JSONArray): List<Pair<Double, Double>> {
        val result = mutableListOf<Pair<Double, Double>>()

        for (i in 0 until geometry.length()) {
            val point = geometry.optJSONObject(i) ?: continue
            val lat = point.optDouble("lat", Double.NaN)
            val lng = point.optDouble("lon", Double.NaN)

            if (lat.isNaN() || lng.isNaN()) continue
            result += Pair(lng, lat)
        }

        if (result.isNotEmpty() && result.first() != result.last()) {
            result += result.first()
        }

        return result
    }

    private fun approximateAreaScore(points: List<Pair<Double, Double>>): Double {
        var area = 0.0
        for (i in 0 until points.size - 1) {
            val (x1, y1) = points[i]
            val (x2, y2) = points[i + 1]
            area += x1 * y2 - x2 * y1
        }
        return abs(area) * 0.5
    }

    private fun zoneDistanceSq(lat: Double, lng: Double, zone: Zone): Double {
        val dx = zone.centerLng - lng
        val dy = zone.centerLat - lat
        return dx * dx + dy * dy
    }

    private fun zonePriority(type: ZoneType): Int {
        return when (type) {
            ZoneType.WATER -> 0
            ZoneType.PARK -> 1
            ZoneType.GARDEN -> 2
            ZoneType.MEADOW -> 3
            ZoneType.FOREST -> 4
            ZoneType.URBAN_GREEN -> 5
            ZoneType.RUINS -> 6
        }
    }

    private fun balanceZones(lat: Double, lng: Double, zones: List<Zone>): List<Zone> {
        val sorted = zones.sortedWith(
            compareBy<Zone>(
                { zonePriority(it.type) },
                { zoneDistanceSq(lat, lng, it) }
            )
        )

        val limits = mapOf(
            ZoneType.WATER to 5,
            ZoneType.PARK to 8,
            ZoneType.GARDEN to 8,
            ZoneType.MEADOW to 6,
            ZoneType.FOREST to 6,
            ZoneType.URBAN_GREEN to 40,
            ZoneType.RUINS to 3
        )

        val counts = mutableMapOf<ZoneType, Int>()
        val result = mutableListOf<Zone>()

        for (zone in sorted) {
            if (result.size >= 40) break

            val currentCount = counts[zone.type] ?: 0
            val limit = limits[zone.type] ?: Int.MAX_VALUE

            if (currentCount >= limit) continue

            counts[zone.type] = currentCount + 1
            result += zone
        }

        return result
    }
}