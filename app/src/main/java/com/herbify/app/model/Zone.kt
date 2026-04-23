package com.herbify.app.model

enum class ZoneType(
    val displayName: String,
    val emoji: String,
    val description: String,
    val rareChance: String,
    val accentColorHex: Long,
    val fillColor: String,
    val possiblePlants: List<String>
) {
    FOREST(
        displayName = "Forest Zone",
        emoji = "🌲",
        description = "Dense woodland with shade-loving and rare wild plants.",
        rareChance = "High — Rare forest plants likely",
        accentColorHex = 0xFF1B5E20,
        fillColor = "#1B5E20",
        possiblePlants = listOf("Fern", "Moss", "Pine", "Wild Garlic", "Chanterelle", "Oak")
    ),
    PARK(
        displayName = "Park Zone",
        emoji = "🌳",
        description = "Urban green area with common and uncommon plants.",
        rareChance = "Medium — Balanced spawn pool",
        accentColorHex = 0xFF43A047,
        fillColor = "#43A047",
        possiblePlants = listOf("Dandelion", "Clover", "Daisy", "Plantain", "Yarrow", "Chamomile")
    ),
    GARDEN(
        displayName = "Garden Zone",
        emoji = "🌷",
        description = "Cultivated green space with decorative and herb-like plants.",
        rareChance = "Medium — Decorative species possible",
        accentColorHex = 0xFF7CB342,
        fillColor = "#7CB342",
        possiblePlants = listOf("Lavender", "Mint", "Rosemary", "Sage", "Thyme", "Chamomile")
    ),
    MEADOW(
        displayName = "Meadow Zone",
        emoji = "🌾",
        description = "Open grassland with flowers and wild herbs.",
        rareChance = "Medium-High — Wildflowers likely",
        accentColorHex = 0xFF9CCC65,
        fillColor = "#9CCC65",
        possiblePlants = listOf("Poppy", "Cornflower", "Thistle", "St. John's Wort", "Sage", "Clover")
    ),
    WATER(
        displayName = "Water Zone",
        emoji = "🌊",
        description = "Wet area near water where moisture-loving plants grow.",
        rareChance = "High — Water plants possible",
        accentColorHex = 0xFF1E88E5,
        fillColor = "#1E88E5",
        possiblePlants = listOf("Reed", "Watercress", "Cattail", "Iris", "Willowherb", "Forget-me-not")
    ),
    URBAN_GREEN(
        displayName = "Urban Green",
        emoji = "🌿",
        description = "Grass strips, courtyards, roadside greenery and rough vegetation.",
        rareChance = "Low-Medium — Hardy plants dominate",
        accentColorHex = 0xFF66BB6A,
        fillColor = "#66BB6A",
        possiblePlants = listOf("Dandelion", "Plantain", "Nettle", "Bindweed", "Groundsel", "Shepherd's purse")
    ),
    RUINS(
        displayName = "Industrial / Ruins",
        emoji = "🏚️",
        description = "Harsh disturbed terrain with resilient species.",
        rareChance = "Medium — Tough rare species possible",
        accentColorHex = 0xFF8D6E63,
        fillColor = "#8D6E63",
        possiblePlants = listOf("Mugwort", "Nettle", "Thistle", "Dock", "Wormwood", "Burdock")
    )
}

data class Zone(
    val id: String,
    val type: ZoneType,
    val coordinates: List<Pair<Double, Double>>,
    val centerLng: Double = coordinates.map { it.first }.average(),
    val centerLat: Double = coordinates.map { it.second }.average()
)

fun Zone.toGeoJsonFeature(selected: Boolean = false): String {
    val coords = coordinates.joinToString(",") { "[${it.first},${it.second}]" }
    return """{"type":"Feature","properties":{"zoneId":"$id","zoneType":"${type.name}","selected":$selected},"geometry":{"type":"Polygon","coordinates":[[$coords]]}}"""
}

fun List<Zone>.toGeoJsonFeatureCollection(selectedId: String? = null): String {
    val features = joinToString(",") { zone ->
        zone.toGeoJsonFeature(selected = zone.id == selectedId)
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

object ZoneDefaults {
    const val BASE_LAT = 48.7164
    const val BASE_LNG = 21.2611

    fun getDefaultCenterLat() = BASE_LAT
    fun getDefaultCenterLng() = BASE_LNG
}