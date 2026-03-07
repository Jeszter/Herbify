package com.example.ecoscanner

import kotlin.math.*

// ─── Biome ────────────────────────────────────────────────────────────────────

enum class Biome(val label: String, val emoji: String) {
    FOREST("Лес", "🌲"),
    MEADOW("Луг", "🌾"),
    WETLAND("Водоём", "🌊"),
    MOUNTAIN("Горы", "⛰️"),
    URBAN("Город", "🏙️"),
    COASTAL("Побережье", "🏖️")
}

// ─── MapObject ────────────────────────────────────────────────────────────────

data class MapObject(
    val id: Int,
    val emoji: String,
    val name: String,
    val rarity: Rarity,
    val lat: Double,
    val lon: Double,
    val scanRadiusM: Int = 30,
    val biome: Biome = Biome.FOREST
) {
    fun toEcoCard(): EcoCard =
        PLANT_DATABASE.firstOrNull { it.name == name }
            ?: EcoCard(
                id          = id,
                emoji       = emoji,
                name        = name,
                latin       = name,
                rarity      = rarity,
                description = "Найдено в реальном мире (${biome.label})"
            )

    fun isInRange(userLat: Double, userLon: Double): Boolean =
        distanceM(userLat, userLon, lat, lon) <= scanRadiusM

    fun isOnCooldown(): Boolean = GameState.plantCdRemaining(id) > 0L
}

// ─── Repository ───────────────────────────────────────────────────────────────

object MapObjectsRepository {

    fun getObjectsNearby(lat: Double, lon: Double, radiusM: Int = GameState.radarRadiusM): List<MapObject> =
        ALL_MAP_OBJECTS
            .filter { distanceM(lat, lon, it.lat, it.lon) <= radiusM }
            .sortedBy { distanceM(lat, lon, it.lat, it.lon) }

    fun nearestScannable(lat: Double, lon: Double): MapObject? =
        ALL_MAP_OBJECTS
            .filter { !it.isOnCooldown() }
            .minByOrNull { distanceM(lat, lon, it.lat, it.lon) }

    fun formatDistance(userLat: Double, userLon: Double, obj: MapObject): String {
        val m = distanceM(userLat, userLon, obj.lat, obj.lon).toInt()
        return if (m >= 1000) "${"%.1f".format(m / 1000.0)} км" else "$m м"
    }

    val ALL_MAP_OBJECTS: List<MapObject> = listOf(
        MapObject(101, "🌺", "Прострел луговой",    Rarity.EPIC,      50.4501, 30.5241, biome = Biome.MEADOW),
        MapObject(102, "🍄", "Мухомор красный",     Rarity.LEGENDARY, 50.4512, 30.5255, biome = Biome.FOREST),
        MapObject(103, "🌿", "Папоротник орляк",    Rarity.RARE,      50.4495, 30.5230, biome = Biome.FOREST),
        MapObject(104, "🪨", "Гранит",              Rarity.RARE,      50.4520, 30.5270, biome = Biome.MOUNTAIN),
        MapObject(105, "🌲", "Ель обыкновенная",    Rarity.COMMON,    50.4480, 30.5215, biome = Biome.FOREST),
        MapObject(106, "🌸", "Цикорий обыкновенный",Rarity.COMMON,    50.4530, 30.5200, biome = Biome.MEADOW),
        MapObject(107, "🎋", "Бамбук обыкновенный", Rarity.EPIC,      50.4468, 30.5280, biome = Biome.URBAN),
        MapObject(108, "🌹", "Шиповник майский",    Rarity.RARE,      50.4545, 30.5240, biome = Biome.MEADOW),
        MapObject(109, "🌻", "Подсолнух",           Rarity.COMMON,    50.4460, 30.5195, biome = Biome.MEADOW),
        MapObject(110, "🍀", "Клевер луговой",      Rarity.COMMON,    50.4510, 30.5265, biome = Biome.MEADOW)
    )
}

// ─── Утилита: расстояние в метрах (Haversine) ────────────────────────────────

fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R    = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a    = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}