package com.example.ecoscanner

// ─── Upgrade System ───────────────────────────────────────────────────────────

enum class UpgradeCategory(val label: String) {
    SCANNER("Scanner"),
    BACKPACK("Backpack"),
    RADAR("Radar"),
    AI("AI Accuracy"),
    ECONOMY("Economy")
}

data class UpgradeLevel(
    val level: Int,
    val cost: Int,
    val effectValue: Long,
    val description: String
)

data class UpgradeItem(
    val id: String,
    val emoji: String,
    val name: String,
    val category: UpgradeCategory,
    val levels: List<UpgradeLevel>
) {
    val maxLevel: Int get() = levels.size
}

// ─── Shop Booster (one-time purchase) ────────────────────────────────────────

data class BoosterItem(
    val id: String,
    val emoji: String,
    val name: String,
    val description: String,
    val cost: Int,
    val effectMs: Long = 0L
)

// ─── Data ─────────────────────────────────────────────────────────────────────

val SHOP_UPGRADES = listOf(

    // ── SCANNER ───────────────────────────────────────────────────────────────
    UpgradeItem(
        id = "scanner_cd", emoji = "⚡", name = "Fast Scanner",
        category = UpgradeCategory.SCANNER,
        levels = listOf(
            UpgradeLevel(1, cost = 50,  effectValue = 30_000L,  "−30 sec CD (remaining: 1m 30s)"),
            UpgradeLevel(2, cost = 100, effectValue = 60_000L,  "−1 min CD (remaining: 1m 00s)"),
            UpgradeLevel(3, cost = 200, effectValue = 90_000L,  "−1.5 min CD (remaining: 30s)"),
            UpgradeLevel(4, cost = 350, effectValue = 110_000L, "−2 min CD (remaining: 10s)"),
            UpgradeLevel(5, cost = 500, effectValue = 120_000L, "No CD — scan instantly (MAX)")
        )
    ),

    UpgradeItem(
        id = "ai_accuracy", emoji = "🔬", name = "AI Accuracy",
        category = UpgradeCategory.AI,
        levels = listOf(
            UpgradeLevel(1, 75,  5L,  "+5% Plant.id accuracy"),
            UpgradeLevel(2, 150, 10L, "+10% · fewer false scans"),
            UpgradeLevel(3, 300, 15L, "+15% · unlocks night scanning"),
            UpgradeLevel(4, 500, 25L, "+25% · detects plant diseases"),
            UpgradeLevel(5, 800, 40L, "+40% · instant AI analysis (MAX)")
        )
    ),

    // ── BACKPACK ──────────────────────────────────────────────────────────────
    UpgradeItem(
        id = "backpack", emoji = "🎒", name = "Backpack",
        category = UpgradeCategory.BACKPACK,
        levels = listOf(
            UpgradeLevel(1, 80,  20L,  "+20 slots (50 total)"),
            UpgradeLevel(2, 120, 50L,  "+50 slots (100 total)"),
            UpgradeLevel(3, 250, 100L, "+100 slots (200 total)"),
            UpgradeLevel(4, 450, 200L, "+200 slots (400 total)"),
            UpgradeLevel(5, 800, -1L,  "Unlimited collection (MAX)")
        )
    ),

    // ── RADAR ─────────────────────────────────────────────────────────────────
    UpgradeItem(
        id = "radar", emoji = "🗺️", name = "Object Radar",
        category = UpgradeCategory.RADAR,
        levels = listOf(
            UpgradeLevel(1, 60,  50L,  "+50 m detection radius"),
            UpgradeLevel(2, 100, 100L, "+100 m · shows rarity in advance"),
            UpgradeLevel(3, 180, 200L, "+200 m · sees already scanned objects"),
            UpgradeLevel(4, 320, 300L, "Auto-navigate to Epic+ objects"),
            UpgradeLevel(5, 600, 500L, "Global radar · no limits (MAX)")
        )
    ),

    // ── ECONOMY ───────────────────────────────────────────────────────────────
    UpgradeItem(
        id = "eco_bonus", emoji = "🪙", name = "ECO Bonus",
        category = UpgradeCategory.ECONOMY,
        levels = listOf(
            UpgradeLevel(1, 100, 10L, "+10% to every reward"),
            UpgradeLevel(2, 200, 25L, "+25% to every reward"),
            UpgradeLevel(3, 400, 50L, "+50% · double event bonus"),
            UpgradeLevel(4, 700, 75L, "+75% · triple quest bonus"),
            UpgradeLevel(5, 1200, 100L, "×2 to all rewards (MAX)")
        )
    )
)

val SHOP_BOOSTERS = listOf(
    BoosterItem(
        id = "cd_reset", emoji = "⏩", name = "Scanner CD Reset",
        description = "Instantly resets the current cooldown",
        cost = 10, effectMs = Long.MAX_VALUE
    ),
    BoosterItem(
        id = "cd_half", emoji = "⚡", name = "CD −50%",
        description = "Halves the current cooldown",
        cost = 5, effectMs = 0L
    ),
    BoosterItem(
        id = "double_eco", emoji = "🪙", name = "×2 ECO for 1 hour",
        description = "Doubles all rewards for one hour",
        cost = 30
    ),
    BoosterItem(
        id = "rare_boost", emoji = "⭐", name = "Rarity ×2 for 30 min",
        description = "Doubles the chance of Rare+ objects on the map",
        cost = 20
    )
)