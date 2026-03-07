package com.example.ecoscanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import kotlin.math.*
import kotlin.random.Random

// ─── Biome spawn rules ────────────────────────────────────────────────────────
// Query Overpass API (OpenStreetMap) for terrain tags and generate objects

object BiomeSpawner {

    // Which plants spawn in which biome
    private val BIOME_PLANTS = mapOf(
        Biome.FOREST  to listOf(3, 4, 8),
        Biome.MEADOW  to listOf(2, 5, 7, 10),
        Biome.WETLAND to listOf(9, 10),
        Biome.MOUNTAIN to listOf(6, 3),
        Biome.URBAN   to listOf(2, 7, 10),
        Biome.COASTAL to listOf(9, 5)
    )

    // Minimum distance between objects (metres)
    private const val MIN_SPACING_M = 40.0

    // ── Query biomes near player via Overpass ─────────────────────────────

    suspend fun fetchBiomesNear(lat: Double, lon: Double, radiusM: Int = 500): List<BiomeArea> =
        withContext(Dispatchers.IO) {
            try {
                val query = """
                    [out:json][timeout:10];
                    (
                      way["leisure"="park"](around:$radiusM,$lat,$lon);
                      way["natural"="wood"](around:$radiusM,$lat,$lon);
                      way["natural"="water"](around:$radiusM,$lat,$lon);
                      way["landuse"="forest"](around:$radiusM,$lat,$lon);
                      way["natural"="grassland"](around:$radiusM,$lat,$lon);
                      way["natural"="heath"](around:$radiusM,$lat,$lon);
                      way["leisure"="nature_reserve"](around:$radiusM,$lat,$lon);
                    );
                    out center;
                """.trimIndent()

                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://overpass-api.de/api/interpreter?data=$encoded"
                val json = URL(url).readText()
                parseOverpassBiomes(json)
            } catch (_: Exception) {
                // Fallback — return default biome
                listOf(BiomeArea(lat, lon, Biome.URBAN, 200.0))
            }
        }

    private fun parseOverpassBiomes(json: String): List<BiomeArea> {
        val result = mutableListOf<BiomeArea>()
        try {
            val root     = JSONObject(json)
            val elements = root.getJSONArray("elements")
            for (i in 0 until elements.length()) {
                val el   = elements.getJSONObject(i)
                val tags = el.optJSONObject("tags") ?: continue
                val center = el.optJSONObject("center") ?: continue
                val lat  = center.optDouble("lat") ?: continue
                val lon  = center.optDouble("lon") ?: continue

                val biome = when {
                    tags.optString("natural") == "water"       -> Biome.WETLAND
                    tags.optString("natural") == "wood"        -> Biome.FOREST
                    tags.optString("landuse") == "forest"      -> Biome.FOREST
                    tags.optString("leisure") == "park"        -> Biome.MEADOW
                    tags.optString("natural") == "grassland"   -> Biome.MEADOW
                    tags.optString("natural") == "heath"       -> Biome.MEADOW
                    tags.optString("leisure") == "nature_reserve" -> Biome.FOREST
                    else                                        -> Biome.URBAN
                }
                result.add(BiomeArea(lat, lon, biome, 150.0))
            }
        } catch (_: Exception) {}
        return result
    }

    // ── Generate objects based on biomes ──────────────────────────────────

    fun spawnObjects(
        playerLat: Double,
        playerLon: Double,
        biomes: List<BiomeArea>,
        count: Int = 12
    ): List<MapObject> {
        val rng     = Random(seed = (playerLat * 1000 + playerLon * 1000).toLong())
        val result  = mutableListOf<MapObject>()
        var idCounter = 200

        fun biomeAt(lat: Double, lon: Double): Biome {
            val nearest = biomes.minByOrNull { distanceM(it.lat, it.lon, lat, lon) }
            return if (nearest != null && distanceM(nearest.lat, nearest.lon, lat, lon) < nearest.radiusM)
                nearest.biome else Biome.URBAN
        }

        var attempts = 0
        while (result.size < count && attempts < count * 10) {
            attempts++
            val angle  = rng.nextDouble() * 2 * PI
            val dist   = rng.nextDouble() * 350 + 50
            val dLat   = (dist * cos(angle)) / 111_320.0
            val dLon   = (dist * sin(angle)) / (111_320.0 * cos(Math.toRadians(playerLat)))
            val lat    = playerLat + dLat
            val lon    = playerLon + dLon

            val tooClose = result.any { distanceM(it.lat, it.lon, lat, lon) < MIN_SPACING_M }
            if (tooClose) continue

            val biome      = biomeAt(lat, lon)
            val plantIds   = BIOME_PLANTS[biome] ?: BIOME_PLANTS[Biome.URBAN]!!
            val plantId    = plantIds[rng.nextInt(plantIds.size)]
            val card       = PLANT_DATABASE.find { it.id == plantId } ?: PLANT_DATABASE.first()

            val rarity = rollRarity(rng)
            val finalCard = if (rarity != card.rarity) {
                card.copy(rarity = rarity)
            } else card

            result.add(MapObject(
                id          = idCounter++,
                emoji       = finalCard.emoji,
                name        = finalCard.name,
                rarity      = finalCard.rarity,
                lat         = lat,
                lon         = lon,
                scanRadiusM = 30,
                biome       = biome
            ))
        }
        return result
    }

    private fun rollRarity(rng: Random): Rarity {
        return when (rng.nextInt(100)) {
            in 0..59  -> Rarity.COMMON
            in 60..84 -> Rarity.RARE
            in 85..96 -> Rarity.EPIC
            else      -> Rarity.LEGENDARY
        }
    }
}

// ─── Helper types ─────────────────────────────────────────────────────────────

data class BiomeArea(
    val lat: Double,
    val lon: Double,
    val biome: Biome,
    val radiusM: Double
)