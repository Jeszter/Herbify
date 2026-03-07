package com.example.ecoscanner

// ─── Upgrade System ───────────────────────────────────────────────────────────

enum class UpgradeCategory(val label: String) {
    SCANNER("Сканер"),
    BACKPACK("Рюкзак"),
    RADAR("Радар"),
    AI("AI Точность"),
    ECONOMY("Экономика")
}

data class UpgradeLevel(
    val level: Int,
    val cost: Int,           // ECO стоимость
    val effectValue: Long,   // значение эффекта (мс / слоты / метры / %)
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

// ─── Shop Booster (разовая покупка) ──────────────────────────────────────────

data class BoosterItem(
    val id: String,
    val emoji: String,
    val name: String,
    val description: String,
    val cost: Int,
    val effectMs: Long = 0L   // на сколько сокращает КД (0 = не влияет на КД)
)

// ─── Data ─────────────────────────────────────────────────────────────────────

val SHOP_UPGRADES = listOf(

    // ── СКАНЕР ────────────────────────────────────────────────────────────────
    UpgradeItem(
        id = "scanner_cd", emoji = "⚡", name = "Быстрый сканер",
        category = UpgradeCategory.SCANNER,
        levels = listOf(
            UpgradeLevel(1, cost = 50,  effectValue = 30_000L,  "−30 сек КД (осталось: 1м 30с)"),
            UpgradeLevel(2, cost = 100, effectValue = 60_000L,  "−1 мин КД (осталось: 1м 00с)"),
            UpgradeLevel(3, cost = 200, effectValue = 90_000L,  "−1.5 мин КД (осталось: 30с)"),
            UpgradeLevel(4, cost = 350, effectValue = 110_000L, "−2 мин КД (осталось: 10с)"),
            UpgradeLevel(5, cost = 500, effectValue = 120_000L, "Нет КД — сканируй мгновенно (MAX)")
        )
    ),

    UpgradeItem(
        id = "ai_accuracy", emoji = "🔬", name = "AI Точность",
        category = UpgradeCategory.AI,
        levels = listOf(
            UpgradeLevel(1, 75,  5L,  "+5% точность Plant.id"),
            UpgradeLevel(2, 150, 10L, "+10% · меньше ошибочных сканов"),
            UpgradeLevel(3, 300, 15L, "+15% · разблокирует ночное сканирование"),
            UpgradeLevel(4, 500, 25L, "+25% · определяет болезни растений"),
            UpgradeLevel(5, 800, 40L, "+40% · мгновенный AI анализ (MAX)")
        )
    ),

    // ── РЮКЗАК ────────────────────────────────────────────────────────────────
    UpgradeItem(
        id = "backpack", emoji = "🎒", name = "Рюкзак",
        category = UpgradeCategory.BACKPACK,
        levels = listOf(
            UpgradeLevel(1, 80,  20L,  "+20 слотов (50 всего)"),
            UpgradeLevel(2, 120, 50L,  "+50 слотов (100 всего)"),
            UpgradeLevel(3, 250, 100L, "+100 слотов (200 всего)"),
            UpgradeLevel(4, 450, 200L, "+200 слотов (400 всего)"),
            UpgradeLevel(5, 800, -1L,  "Безлимитная коллекция (MAX)")
        )
    ),

    // ── РАДАР ─────────────────────────────────────────────────────────────────
    UpgradeItem(
        id = "radar", emoji = "🗺️", name = "Радар объектов",
        category = UpgradeCategory.RADAR,
        levels = listOf(
            UpgradeLevel(1, 60,  50L,  "+50м радиус обнаружения"),
            UpgradeLevel(2, 100, 100L, "+100м · показывает редкость заранее"),
            UpgradeLevel(3, 180, 200L, "+200м · видит уже отсканированные"),
            UpgradeLevel(4, 320, 300L, "Авто-навигация к Epic+ объектам"),
            UpgradeLevel(5, 600, 500L, "Глобальный радар · без ограничений (MAX)")
        )
    ),

    // ── ЭКОНОМИКА ─────────────────────────────────────────────────────────────
    UpgradeItem(
        id = "eco_bonus", emoji = "🪙", name = "ECO Бонус",
        category = UpgradeCategory.ECONOMY,
        levels = listOf(
            UpgradeLevel(1, 100, 10L, "+10% к каждому награждению"),
            UpgradeLevel(2, 200, 25L, "+25% к каждому награждению"),
            UpgradeLevel(3, 400, 50L, "+50% · двойной ивент-бонус"),
            UpgradeLevel(4, 700, 75L, "+75% · тройной квест-бонус"),
            UpgradeLevel(5, 1200, 100L, "×2 ко всем наградам (MAX)")
        )
    )
)

val SHOP_BOOSTERS = listOf(
    BoosterItem(
        id = "cd_reset", emoji = "⏩", name = "Сброс КД сканера",
        description = "Мгновенно сбрасывает текущую перезарядку",
        cost = 10, effectMs = Long.MAX_VALUE
    ),
    BoosterItem(
        id = "cd_half", emoji = "⚡", name = "КД −50%",
        description = "Уменьшает текущий КД вдвое",
        cost = 5, effectMs = 0L   // логика в GameState.applyBooster()
    ),
    BoosterItem(
        id = "double_eco", emoji = "🪙", name = "×2 ECO на 1 час",
        description = "Удваивает все награды в течение часа",
        cost = 30
    ),
    BoosterItem(
        id = "rare_boost", emoji = "⭐", name = "Редкость ×2 на 30 мин",
        description = "Удваивает шанс Rare+ объектов на карте",
        cost = 20
    )
)