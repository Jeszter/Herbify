package com.herbify.app.model

// PlantObject — no map SDK dependency, position is optional (lat/lng pair)
data class PlantObject(
    val id: String,
    val name: String,
    val latinName: String,
    val description: String,
    val rarity: Rarity,
    val ecoReward: Int,
    val xpReward: Int,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val emoji: String
)

enum class Rarity(val label: String, val color: Long) {
    COMMON("Common",       0xFF7BA87B),
    UNCOMMON("Uncommon",   0xFF39FF14),
    RARE("Rare",           0xFF4488FF),
    EPIC("Epic",           0xFFAA44FF),
    LEGENDARY("Legendary", 0xFFFFD700)
}

object MapObjects {
    val plants = listOf(
        PlantObject("plant_001", "Dandelion",       "Taraxacum officinale",   "Rich in vitamins A, C, and K.",                            Rarity.COMMON,    10,  15,  48.7164, 21.2611, "🌼"),
        PlantObject("plant_002", "Chamomile",       "Matricaria chamomilla",  "Gentle herb with sweet apple-like scent.",                 Rarity.UNCOMMON,  20,  25,  48.7180, 21.2630, "🌸"),
        PlantObject("plant_003", "Lavender",        "Lavandula angustifolia", "Fragrant herb for relaxation and aromatherapy.",           Rarity.UNCOMMON,  25,  30,  48.7150, 21.2595, "💜"),
        PlantObject("plant_004", "Valerian",        "Valeriana officinalis",  "Rare medicinal herb for insomnia and nerve pain.",         Rarity.RARE,      50,  60,  48.7200, 21.2650, "🌿"),
        PlantObject("plant_005", "Echinacea",       "Echinacea purpurea",     "Powerful immune-boosting herb.",                          Rarity.RARE,      55,  65,  48.7140, 21.2580, "🌺"),
        PlantObject("plant_006", "Golden Seal",     "Hydrastis canadensis",   "Epic herb with antimicrobial properties.",                Rarity.EPIC,      100, 120, 48.7220, 21.2670, "🌱"),
        PlantObject("plant_007", "Dragon's Blood",  "Dracaena draco",         "Legendary tree with red resin used in ancient medicine.", Rarity.LEGENDARY, 250, 300, 48.7130, 21.2560, "🐉"),
        PlantObject("plant_008", "Mint",            "Mentha piperita",        "Refreshing aromatic herb.",                               Rarity.COMMON,    10,  15,  48.7170, 21.2640, "🌿"),
        PlantObject("plant_009", "St. John's Wort", "Hypericum perforatum",   "Used to treat mild depression and nerve pain.",           Rarity.UNCOMMON,  22,  28,  48.7190, 21.2600, "⭐"),
        PlantObject("plant_010", "Ginseng",         "Panax ginseng",          "Legendary root for energy and adaptogenic properties.",   Rarity.LEGENDARY, 280, 320, 48.7210, 21.2555, "🫚")
    )
}
